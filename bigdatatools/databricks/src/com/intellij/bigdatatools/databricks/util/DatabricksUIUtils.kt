package com.intellij.bigdatatools.databricks.util

import com.intellij.openapi.util.NlsContexts
import com.intellij.ui.dsl.builder.BottomGap
import com.intellij.ui.dsl.builder.Panel
import com.intellij.ui.dsl.builder.Row
import com.intellij.ui.dsl.builder.TopGap
import com.jetbrains.bigdatatools.common.rfs.driver.metainfo.components.SelectableLabel

fun Panel.labeledSelectableLabel(@NlsContexts.Label label: String, @NlsContexts.Label text: String) {
  row(label) {
    cell(SelectableLabel(text))
  }.noGap()
}

fun Row.noGap(): Row {
  return topGap(TopGap.NONE).bottomGap(BottomGap.NONE)
}