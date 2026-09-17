// ==========================================================
// Config & Constants
// ==========================================================
const API_BASE = 'http://localhost:8080';
const STORAGE_TOKEN_KEY = 'ledger_token';
const STORAGE_USER_KEY = 'ledger_user';

// ==========================================================
// Authentication Guard
// ==========================================================
function getToken() {
  return localStorage.getItem(STORAGE_TOKEN_KEY);
}

function getUser() {
  const raw = localStorage.getItem(STORAGE_USER_KEY);
  try {
    return raw ? JSON.parse(raw) : null;
  } catch (_) {
    return null;
  }
}

function clearSessionAndRedirect() {
  localStorage.removeItem(STORAGE_TOKEN_KEY);
  localStorage.removeItem(STORAGE_USER_KEY);
  window.location.replace('index.html');
}

// Redirect immediately if not authenticated
if (!getToken()) {
  clearSessionAndRedirect();
}

// ==========================================================
// Element references
// ==========================================================
const userNameEl = document.getElementById('userName');
const userRoleEl = document.getElementById('userRole');
const logoutBtn = document.getElementById('logoutBtn');
const appError = document.getElementById('appError');

const accountSelect = document.getElementById('accountSelect');
const yearInput = document.getElementById('yearInput');
const monthSelect = document.getElementById('monthSelect');
const thisMonthBtn = document.getElementById('thisMonthBtn');
const lastMonthBtn = document.getElementById('lastMonthBtn');
const viewStatementBtn = document.getElementById('viewStatementBtn');

const historyList = document.getElementById('historyList');
const statementPlaceholder = document.getElementById('statementPlaceholder');
const statementSheet = document.getElementById('statementSheet');

const sheetPeriod = document.getElementById('sheetPeriod');
const sheetStatus = document.getElementById('sheetStatus');
const sheetHolder = document.getElementById('sheetHolder');
const sheetAccountNumber = document.getElementById('sheetAccountNumber');
const sheetPeriodEnd = document.getElementById('sheetPeriodEnd');
const sheetOpening = document.getElementById('sheetOpening');
const sheetCredits = document.getElementById('sheetCredits');
const sheetDebits = document.getElementById('sheetDebits');
const sheetFees = document.getElementById('sheetFees');
const sheetInterest = document.getElementById('sheetInterest');
const sheetClosing = document.getElementById('sheetClosing');
const linesTableBody = document.getElementById('linesTableBody');
const downloadPdfBtn = document.getElementById('downloadPdfBtn');

const adminPanel = document.getElementById('adminPanel');
const adminYear = document.getElementById('adminYear');
const adminMonth = document.getElementById('adminMonth');
const runBatchBtn = document.getElementById('runBatchBtn');
const batchResult = document.getElementById('batchResult');

// ==========================================================
// State
// ==========================================================
let currentAccountId = null;
let currentYear = null;
let currentMonth = null;

// ==========================================================
// Sign out
// ==========================================================
logoutBtn.addEventListener('click', () => {
  clearSessionAndRedirect();
});

// ==========================================================
// API helper
// ==========================================================
async function api(path, options = {}) {
  const headers = Object.assign({}, options.headers || {});
  const token = getToken();
  if (token) {
    headers['Authorization'] = `Bearer ${token}`;
  }

  const response = await fetch(`${API_BASE}${path}`, { ...options, headers });

  if (response.status === 401) {
    clearSessionAndRedirect();
    throw new Error('Session expired - please sign in again.');
  }

  if (!response.ok) {
    let message = `Request failed (${response.status})`;
    try {
      const body = await response.json();
      if (body.message) message = body.message;
    } catch (_) { /* non-JSON response */ }
    throw new Error(message);
  }

  return response;
}

function showAppError(message) {
  appError.textContent = message;
  appError.hidden = false;
}

function clearAppError() {
  appError.textContent = '';
  appError.hidden = true;
}

// ==========================================================
// Accounts loader
// ==========================================================
async function loadAccounts() {
  const response = await api('/api/accounts');
  const accounts = await response.json();

  accountSelect.innerHTML = '';
  accounts.forEach(acc => {
    const option = document.createElement('option');
    option.value = acc.id;
    const owner = acc.ownerName ? ` — ${acc.ownerName}` : '';
    option.textContent = `${acc.accountNumber} (${acc.accountType})${owner} [${acc.currency}]`;
    accountSelect.appendChild(option);
  });

  if (accounts.length > 0) {
    // Preserve selected account if already chosen
    if (!currentAccountId || !accounts.some(a => String(a.id) === String(currentAccountId))) {
      currentAccountId = accounts[0].id;
    }
    accountSelect.value = currentAccountId;
    await loadHistory();
  } else {
    currentAccountId = null;
    accountSelect.innerHTML = '<option value="">No accounts available</option>';
    historyList.innerHTML = '<li class="history-empty">No accounts registered in the system.</li>';
  }
}

accountSelect.addEventListener('change', async () => {
  currentAccountId = accountSelect.value;
  resetStatementView();
  try {
    await loadHistory();
  } catch (err) {
    showAppError(err.message);
  }
});

// ==========================================================
// Statement history
// ==========================================================
async function loadHistory() {
  if (!currentAccountId) return;
  const response = await api(`/api/accounts/${currentAccountId}/statements`);
  const statements = await response.json();

  historyList.innerHTML = '';

  if (statements.length === 0) {
    historyList.innerHTML = '<li class="history-empty">No statements generated yet for this account.</li>';
    return;
  }

  statements.forEach(s => {
    const li = document.createElement('li');
    li.className = 'history-item';
    if (currentYear === s.periodYear && currentMonth === s.periodMonth) {
      li.classList.add('active');
    }

    const button = document.createElement('button');
    button.innerHTML = `<strong>${monthName(s.periodMonth)} ${s.periodYear}</strong>` +
      `<span class="history-balance">Bal: ${formatMoney(s.closingBalance)}</span>`;
    button.addEventListener('click', () => {
      yearInput.value = s.periodYear;
      monthSelect.value = String(s.periodMonth);
      fetchAndRenderStatement(s.periodYear, s.periodMonth);
    });

    li.appendChild(button);
    historyList.appendChild(li);
  });
}

function highlightActiveHistory(year, month) {
  const items = historyList.querySelectorAll('.history-item');
  items.forEach(item => {
    const text = item.textContent || '';
    if (text.includes(`${monthName(month)} ${year}`)) {
      item.classList.add('active');
    } else {
      item.classList.remove('active');
    }
  });
}

// ==========================================================
// View statement
// ==========================================================
viewStatementBtn.addEventListener('click', () => {
  const year = parseInt(yearInput.value, 10);
  const month = parseInt(monthSelect.value, 10);
  fetchAndRenderStatement(year, month);
});

thisMonthBtn.addEventListener('click', () => {
  const now = new Date();
  yearInput.value = now.getFullYear();
  monthSelect.value = String(now.getMonth() + 1);
  fetchAndRenderStatement(now.getFullYear(), now.getMonth() + 1);
});

lastMonthBtn.addEventListener('click', () => {
  const now = new Date();
  const lastMonth = new Date(now.getFullYear(), now.getMonth() - 1, 1);
  yearInput.value = lastMonth.getFullYear();
  monthSelect.value = String(lastMonth.getMonth() + 1);
  fetchAndRenderStatement(lastMonth.getFullYear(), lastMonth.getMonth() + 1);
});

async function fetchAndRenderStatement(year, month) {
  if (!currentAccountId) {
    showAppError('Please select an account first.');
    return;
  }
  clearAppError();
  viewStatementBtn.disabled = true;
  viewStatementBtn.textContent = 'Loading...';

  try {
    const response = await api(`/api/accounts/${currentAccountId}/statements/${year}/${month}`);
    const statement = await response.json();
    currentYear = year;
    currentMonth = month;
    renderStatement(statement);
    highlightActiveHistory(year, month);
    // Refresh history sidebar in case a new statement chain was generated
    await loadHistory();
  } catch (err) {
    showAppError(err.message);
  } finally {
    viewStatementBtn.disabled = false;
    viewStatementBtn.textContent = 'View statement';
  }
}

function resetStatementView() {
  statementPlaceholder.hidden = false;
  statementSheet.hidden = true;
  currentYear = null;
  currentMonth = null;
}

function renderStatement(statement) {
  statementPlaceholder.hidden = true;
  statementSheet.hidden = false;

  sheetPeriod.textContent = `${monthName(statement.periodMonth)} ${statement.periodYear}`;
  sheetStatus.textContent = statement.status;
  sheetStatus.className = 'status-pill ' + (statement.status === 'GENERATED' ? 'generated' : 'provisional');

  sheetAccountNumber.textContent = statement.accountNumber;
  sheetHolder.textContent = statement.accountHolder || getUser()?.name || '—';
  sheetPeriodEnd.textContent = statement.periodEndDate;

  sheetOpening.textContent = formatMoney(statement.openingBalance);
  sheetCredits.textContent = '+' + formatMoney(statement.totalCredits);
  sheetDebits.textContent = '-' + formatMoney(statement.totalDebits);
  sheetFees.textContent = '-' + formatMoney(statement.totalFees);
  const interestVal = parseFloat(statement.totalInterest || 0);
  const interestLine = (statement.lines || []).find(l => l.sourceType === 'INTEREST');
  const isInterestDebit = interestLine ? interestLine.dcIndicator === 'DEBIT' : false;

  sheetInterest.className = 'totals-value';
  if (interestVal === 0) {
    sheetInterest.textContent = '+0.00';
  } else if (isInterestDebit) {
    sheetInterest.textContent = '-' + formatMoney(statement.totalInterest);
    sheetInterest.classList.add('amount-debit');
  } else {
    sheetInterest.textContent = '+' + formatMoney(statement.totalInterest);
    sheetInterest.classList.add('amount-credit');
  }
  sheetClosing.textContent = formatMoney(statement.closingBalance);

  linesTableBody.innerHTML = '';
  if (!statement.lines || statement.lines.length === 0) {
    linesTableBody.innerHTML = '<tr><td colspan="5" class="lines-empty">No activity recorded for this period.</td></tr>';
  } else {
    statement.lines.forEach(line => {
      const tr = document.createElement('tr');
      const isDebit = line.dcIndicator === 'DEBIT';
      tr.innerHTML = `
        <td>${line.date}</td>
        <td>${escapeHtml(line.description || '')}</td>
        <td>${line.sourceType}</td>
        <td class="align-right ${isDebit ? 'amount-debit' : 'amount-credit'}">${isDebit ? '-' : '+'}${formatMoney(line.amount)}</td>
        <td class="align-right">${formatMoney(line.balanceAfter)}</td>
      `;
      linesTableBody.appendChild(tr);
    });
  }
}

// ==========================================================
// PDF Download
// ==========================================================
downloadPdfBtn.addEventListener('click', async () => {
  if (!currentAccountId || !currentYear || !currentMonth) return;

  downloadPdfBtn.disabled = true;
  downloadPdfBtn.textContent = 'Generating PDF...';

  try {
    const response = await api(`/api/accounts/${currentAccountId}/statements/${currentYear}/${currentMonth}/pdf`);
    const blob = await response.blob();

    const url = window.URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `statement-${currentYear}-${String(currentMonth).padStart(2, '0')}.pdf`;
    document.body.appendChild(a);
    a.click();
    a.remove();
    window.URL.revokeObjectURL(url);
  } catch (err) {
    showAppError(err.message);
  } finally {
    downloadPdfBtn.disabled = false;
    downloadPdfBtn.textContent = 'Download PDF';
  }
});

// ==========================================================
// Admin batch trigger
// ==========================================================
if (runBatchBtn) {
  runBatchBtn.addEventListener('click', async () => {
    const year = parseInt(adminYear.value, 10);
    const month = parseInt(adminMonth.value, 10);

    const now = new Date();
    const curY = now.getFullYear();
    const curM = now.getMonth() + 1;

    if (year > curY || (year === curY && month >= curM)) {
      batchResult.innerHTML = `<div class="error-text">⚠️ Cannot run batch for current or future period (${monthName(month)} ${year}). Please select a completed month (e.g. August 2026 or earlier).</div>`;
      batchResult.hidden = false;
      return;
    }

    batchResult.hidden = true;
    runBatchBtn.disabled = true;
    runBatchBtn.textContent = 'Running batch...';

    try {
      const response = await api(`/api/admin/batch/statements/run?year=${year}&month=${month}`, { method: 'POST' });
      const result = await response.json();

      const badgeClass = result.failed === 0 ? 'generated' : 'provisional';
      batchResult.innerHTML = `
        <div style="margin-bottom: 0.5rem; display: flex; align-items: center; gap: 0.75rem;">
          <span class="status-pill ${badgeClass}">${result.failed === 0 ? 'SUCCESS' : 'WARNING'}</span>
          <span><strong>Period:</strong> ${result.period} | <strong>Total Accounts:</strong> ${result.totalAccounts} | <strong>Succeeded:</strong> ${result.succeeded} | <strong>Failed:</strong> ${result.failed}</span>
        </div>
        ${result.failureDetails && result.failureDetails.length > 0 ? `<pre style="color: var(--debit); margin-top: 0.5rem;">${escapeHtml(result.failureDetails.join('\n'))}</pre>` : ''}
      `;
      batchResult.hidden = false;

      // Reload accounts and history after generation
      await loadAccounts();
      if (currentAccountId) {
        await loadHistory();
      }
    } catch (err) {
      batchResult.innerHTML = `<div class="error-text">Error: ${escapeHtml(err.message)}</div>`;
      batchResult.hidden = false;
    } finally {
      runBatchBtn.disabled = false;
      runBatchBtn.textContent = 'Run batch';
    }
  });
}

// ==========================================================
// Formatting helpers
// ==========================================================
function formatMoney(value) {
  const num = typeof value === 'number' ? value : parseFloat(value || 0);
  return num.toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 });
}

function monthName(monthNumber) {
  const names = ['January','February','March','April','May','June',
                 'July','August','September','October','November','December'];
  return names[monthNumber - 1] || monthNumber;
}

function escapeHtml(str) {
  const div = document.createElement('div');
  div.textContent = str;
  return div.innerHTML;
}

// ==========================================================
// Bootstrap
// ==========================================================
(async function init() {
  const user = getUser();
  if (user) {
    userNameEl.textContent = user.name;
    const roles = (user.roles || '').split(',').map(r => r.trim());
    const isAdmin = roles.includes('ADMIN');
    userRoleEl.textContent = isAdmin ? 'ADMIN' : 'USER';
    if (adminPanel) {
      adminPanel.hidden = !isAdmin;
    }
  }

  const now = new Date();
  yearInput.value = now.getFullYear();
  monthSelect.value = String(now.getMonth() + 1);

  // Admin batch must default to the PREVIOUS COMPLETED month
  const lastCompleted = new Date(now.getFullYear(), now.getMonth() - 1, 1);
  if (adminYear) adminYear.value = lastCompleted.getFullYear();
  if (adminMonth) adminMonth.value = String(lastCompleted.getMonth() + 1);

  try {
    await loadAccounts();
  } catch (err) {
    showAppError(err.message);
  }
})();
