(function () {
  var t = null;
  try { t = localStorage.getItem('atelier.theme'); } catch (e) {}
  if (t !== 'dark' && t !== 'light') {
    t = window.matchMedia && window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light';
  }
  document.documentElement.dataset.theme = t;
  if (t === 'dark') document.querySelector('meta[name="theme-color"]').setAttribute('content', '#08090a');
})();
