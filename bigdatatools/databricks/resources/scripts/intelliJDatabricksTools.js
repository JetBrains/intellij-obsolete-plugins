if (window.intelliJDatabricksTools === undefined) {
  window.intelliJDatabricksTools = {}
}

/**
 * Toggles visibility of code in results. Can be called only after complete load of notebook.
 * @param visible if true, code cell will become visible.
 */
window.intelliJDatabricksTools.toggleCodeCells = function (visible) {
  let cells = document.getElementsByClassName('command-input previousPrompt')
  for (const cell of cells) {
    cell.style.display = visible ? 'block' : 'none'
  }
  return cells.length !== 0
}

window.intelliJDatabricksTools.toggleGeneratedCell = function (visible) {
  const foundCell = document.querySelector("[aria-label='Cell 1']");
  if (foundCell !== undefined) {
    foundCell.style.display = visible ? 'block' : 'none'
  }

  return foundCell !== undefined
}