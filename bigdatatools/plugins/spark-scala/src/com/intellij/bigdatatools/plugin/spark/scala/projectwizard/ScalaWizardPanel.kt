package com.intellij.bigdatatools.plugin.spark.scala.projectwizard

import com.intellij.openapi.observable.util.transform
import com.intellij.openapi.ui.ComboBox
import com.intellij.ui.dsl.builder.Cell
import com.intellij.ui.dsl.builder.Panel
import com.intellij.ui.dsl.builder.Row
import com.intellij.ui.dsl.builder.RowsRange
import com.intellij.bigdatatools.coreUi.fields.CustomListCellRenderer
import com.jetbrains.bigdatatools.wizard.LanguageWizardPanel
import com.jetbrains.bigdatatools.wizard.SparkProjectWizardStep
import com.jetbrains.bigdatatools.wizard.WizardTemplateConst
import com.jetbrains.bigdatatools.wizard.util.WizardMessageBundle
import scala.collection.JavaConverters

internal class ScalaWizardPanel : LanguageWizardPanel {
  private var scalaComboBox: Cell<ComboBox<String>>? = null
  private var sbtComboBox: Cell<ComboBox<String>>? = null

  override fun initComponent(panel: Panel, wizard: SparkProjectWizardStep): RowsRange {
    return panel.rowsRange {
      row(WizardMessageBundle.message("bdt.wizard.scala.version")) {
        scalaComboBox = createScalaVersionCombobox(this)
      }
      row(WizardMessageBundle.message("bdt.wizard.sbt.version")) {
        sbtComboBox = createSbtVersionCombobox(this)
      }.visibleIf(wizard.buildSystemProperty.transform { it is SparkSbtStructureProviderFactory })
    }
  }

  override fun getValues(): List<Pair<String, String>> = listOf(
    WizardTemplateConst.SCALA_FULL_VERSION to (scalaComboBox?.component?.item ?: WizardTemplateConst.SCALA_FULL_VERSION_DEFAULT),
    WizardTemplateConst.SCALA_VERSIONS_SHORT to (toShortScalaVersion()),
    WizardTemplateConst.SBT_VERSION to (sbtComboBox?.component?.item ?: WizardTemplateConst.SBT_VERSION_DEFAULT))

  private fun toShortScalaVersion() = (scalaComboBox?.component?.item
                                       ?: WizardTemplateConst.SCALA_FULL_VERSION_DEFAULT).dropLastWhile { it != '.' }.dropLast(1)

  private fun createScalaVersionCombobox(row: Row) =
    row.comboBox(JavaConverters.seqAsJavaList(ScalaSparkProjectLanguageHandler.Utils.scalaVersions),
                 CustomListCellRenderer<String> { it }).also {
      it.component.item = WizardTemplateConst.SCALA_FULL_VERSION_DEFAULT
    }

  private fun createSbtVersionCombobox(row: Row) =
    row.comboBox(JavaConverters.seqAsJavaList(ScalaSparkProjectLanguageHandler.Utils.sbtVersions),
                 CustomListCellRenderer<String> { it }).also {
      it.component.item = WizardTemplateConst.SBT_VERSION_DEFAULT
    }
}