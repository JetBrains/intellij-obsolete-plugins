window.addEventListener("load", (event) => {
  let iterations = 0
  const interval = setInterval(() => {
    if (document.getElementById('topbar') !== undefined && document.getElementById('overallView') !== undefined) {
      clearInterval(interval)

      document.getElementById('topbar').style.display = 'none'
      document.getElementById('overallView').style.top = 0

      let whitespaceDiv = document.querySelector('[data-testid="extra-whitespace"]')
      if (whitespaceDiv !== undefined) {
        whitespaceDiv.style.display = 'none'
      }
    }
    iterations++
    if (iterations > 100) {
      clearInterval(interval)
    }
  }, 100)
})