package com.intellij.bigdatatools.notebooks.core.api.nbformat

interface NotebookOutputBuilder {
  fun build(): NotebookOutput?
}