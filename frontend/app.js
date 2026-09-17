// Backward compatibility redirector
(function() {
  const token = localStorage.getItem('ledger_token');
  if (token) {
    window.location.replace('dashboard.html');
  } else {
    window.location.replace('index.html');
  }
})();
