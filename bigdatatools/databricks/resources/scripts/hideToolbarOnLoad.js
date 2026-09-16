window.addEventListener("load", (event) => {
  let iterations = 0
  const interval = setInterval(() => {

    const toolbarStickies = document.querySelectorAll("[data-notebook-cell-toolbar-sticky='true']")

    if (toolbarStickies !== undefined && toolbarStickies.length !== 0) {
      for (const toolbarSticky of toolbarStickies) {
        toolbarSticky.style.display = 'none'
      }
    }

    const toolbarShadows = document.querySelectorAll("[data-notebook-cell-toolbar-shadow='true']")
    if (toolbarShadows !== undefined && toolbarShadows.length !== 0) {
      for (const toolbarShadow of toolbarShadows) {
        toolbarShadow.style.display = 'none'
      }
    }

    iterations++
    if (iterations > 100) {
      clearInterval(interval)
    }
  }, 100)
})