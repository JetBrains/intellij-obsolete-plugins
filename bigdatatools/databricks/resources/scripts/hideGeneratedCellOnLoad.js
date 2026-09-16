/* Hides first, generated cell, on notebook load. */
window.addEventListener("load", (event) => {
  let iterations = 0
  const interval = setInterval(() => {
    if (window.intelliJDatabricksTools.toggleGeneratedCell(false)) {
      clearInterval(interval)
    }
    iterations++
    if (iterations > 100) {
      clearInterval(interval)
    }
  }, 100)
})