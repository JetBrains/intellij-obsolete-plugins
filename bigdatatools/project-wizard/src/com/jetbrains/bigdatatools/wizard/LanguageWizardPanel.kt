package com.jetbrains.bigdatatools.wizard

import com.intellij.ui.dsl.builder.Panel
import com.intellij.ui.dsl.builder.RowsRange

interface LanguageWizardPanel {
  fun initComponent(panel: Panel, wizard: SparkProjectWizardStep): RowsRange
  fun getValues(): List<Pair<String, String>>
}