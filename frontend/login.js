const API_BASE = 'http://localhost:8080';
const STORAGE_TOKEN_KEY = 'ledger_token';
const STORAGE_USER_KEY = 'ledger_user';

const loginForm = document.getElementById('loginForm');
const emailInput = document.getElementById('email');
const passwordInput = document.getElementById('password');
const submitBtn = document.getElementById('submitBtn');
const loginError = document.getElementById('loginError');

// If already signed in, redirect straight to dashboard
(function checkExistingSession() {
  const token = localStorage.getItem(STORAGE_TOKEN_KEY);
  if (token) {
    window.location.replace('dashboard.html');
  }
})();

// Demo chip autofill
document.querySelectorAll('.demo-chip').forEach(chip => {
  chip.addEventListener('click', () => {
    emailInput.value = chip.getAttribute('data-email');
    passwordInput.value = chip.getAttribute('data-password');
    loginError.hidden = true;
    emailInput.focus();
  });
});

// Handle login submit
loginForm.addEventListener('submit', async (e) => {
  e.preventDefault();
  loginError.hidden = true;
  loginError.textContent = '';

  const email = emailInput.value.trim();
  const password = passwordInput.value;

  submitBtn.disabled = true;
  submitBtn.textContent = 'Signing in...';

  try {
    const response = await fetch(`${API_BASE}/api/auth/login`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ email, password })
    });

    if (!response.ok) {
      const body = await response.json().catch(() => ({}));
      throw new Error(body.message || 'Invalid email or password');
    }

    const data = await response.json();
    localStorage.setItem(STORAGE_TOKEN_KEY, data.token);
    localStorage.setItem(STORAGE_USER_KEY, JSON.stringify({
      userId: data.userId,
      name: data.name,
      roles: data.roles
    }));

    // Redirect to the dashboard page
    window.location.href = 'dashboard.html';
  } catch (err) {
    loginError.textContent = err.message;
    loginError.hidden = false;
    submitBtn.disabled = false;
    submitBtn.textContent = 'Sign in';
  }
});
