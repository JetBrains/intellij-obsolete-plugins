package com.intellij.bigdatatools.plugin.spark.python.inlay

import com.intellij.bigdatatools.plugin.spark.assistance.dfanalysis.inlay.DataframeInlayCollector
import com.intellij.bigdatatools.plugin.spark.assistance.dfanalysis.inlay.DataframeInlayPosition
import com.intellij.bigdatatools.plugin.spark.assistance.dfanalysis.inlay.DataframeSampleFileService.Companion.SUPPORTED_READ_METHODS
import com.intellij.bigdatatools.plugin.spark.assistance.statistic.SparkStatisticPython
import com.intellij.bigdatatools.plugin.spark.assistance.util.SAMessagesBundle
import com.intellij.bigdatatools.plugin.spark.assistance.util.SparkAssistanceRegistry
import com.intellij.bigdatatools.plugin.spark.textIn
import com.intellij.codeInsight.hints.ChangeListener
import com.intellij.codeInsight.hints.ImmediateConfigurable
import com.intellij.codeInsight.hints.InlayHintsCollector
import com.intellij.codeInsight.hints.InlayHintsProvider
import com.intellij.codeInsight.hints.InlayHintsSink
import com.intellij.codeInsight.hints.NoSettings
import com.intellij.codeInsight.hints.SettingsKey
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiReferenceService
import com.intellij.psi.createSmartPointer
import com.intellij.psi.impl.source.tree.LeafPsiElement
import com.intellij.psi.util.parentOfType
import com.intellij.psi.util.parents
import com.jetbrains.python.psi.PyCallExpression
import com.jetbrains.python.psi.PyClass
import com.jetbrains.python.psi.PyQualifiedExpression
import com.jetbrains.python.psi.PyReferenceExpression
import com.jetbrains.python.psi.PyStringLiteralExpression
import org.intellij.lang.annotations.Language
import javax.swing.JPanel

internal class PythonDataframeInlayHintsProvider : InlayHintsProvider<NoSettings> {
  override fun getCollectorFor(file: PsiFile,
                               editor: Editor,
                               settings: NoSettings,
                               sink: InlayHintsSink): InlayHintsCollector {
    return PythonDataframeInlayCollector(editor)
  }

  override fun createSettings() = NoSettings()

  override val name: String
    get() = SAMessagesBundle.message("dataframe.attach.inlay.provider.name")
  override val description: String
    get() = SAMessagesBundle.message("dataframe.attach.inlay.provider.description")
  override val key: SettingsKey<NoSettings>
    get() = SparkAssistanceRegistry.SETTINGS_KEY_PYTHON
  override val previewText: String
    @Language("Python")
    get() = """
      from pyspark.sql import SparkSession

      spark = SparkSession.builder.getOrCreate()
      spark.read.parquet("sample.parquet")
    """.trimIndent()

  override fun createConfigurable(settings: NoSettings): ImmediateConfigurable {
    return object : ImmediateConfigurable {
      override fun createComponent(listener: ChangeListener) = JPanel()
    }
  }

}

class PythonDataframeInlayCollector(editor: Editor) : DataframeInlayCollector(editor) {
  override fun getReadCallArguments(reference: PsiElement): DataframeInlayPosition? {
    if (reference !is LeafPsiElement) return null
    if (!reference.textIn(SUPPORTED_READ_METHODS)) return null
    val references = PsiReferenceService.getService().getReferences(reference.parent, PsiReferenceService.Hints.NO_HINTS)
    val resolved = references.any {
      val target = it.resolve()
      target != null && target.parentOfType<PyClass>()?.name == "DataFrameReader"
    }
    if (!resolved) return null
    val qualifiedExpression = reference.parent as? PyQualifiedExpression ?: return null
    val readerExpr = qualifiedExpression.qualifier ?: return null
    if (readerExpr is PyCallExpression && readerExpr.callee?.name == "schema") return null
    val callExpression = reference.parents(false).takeWhile { it is PyReferenceExpression }.lastOrNull()?.parent as? PyCallExpression

    return DataframeInlayPosition(
      readerExpr.textRange,
      qualifiedExpression.createSmartPointer(),
      SparkStatisticPython,
      callExpression?.arguments?.firstOrNull() is PyStringLiteralExpression
    )
  }
}