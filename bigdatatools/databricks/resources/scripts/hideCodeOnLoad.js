/* Hides code blocks on notebook load. */
window.addEventListener("load", (event) => {
  let iterations = 0
  const interval = setInterval(() => {
    if (window.intelliJDatabricksTools.toggleCodeCells(false)) {
      clearInterval(interval)
    }
    iterations++
    if (iterations > 100) {
      clearInterval(interval)
    }
  }, 100)
})