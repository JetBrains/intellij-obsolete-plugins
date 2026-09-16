package com.intellij.bigdatatools.plugin.spark.scala.dfanalysis.inlay

import com.intellij.bigdatatools.plugin.spark.assistance.dfanalysis.inlay.DataframeInlayCollector
import com.intellij.bigdatatools.plugin.spark.assistance.dfanalysis.inlay.DataframeInlayPosition
import com.intellij.bigdatatools.plugin.spark.assistance.dfanalysis.inlay.DataframeSampleFileService
import com.intellij.bigdatatools.plugin.spark.assistance.statistic.SparkStatisticScala
import com.intellij.bigdatatools.plugin.spark.assistance.util.SAMessagesBundle
import com.intellij.bigdatatools.plugin.spark.assistance.util.SparkAssistanceRegistry
import com.intellij.bigdatatools.plugin.spark.scala.dfanalysis.impl.DfAbstractMethodBasedTypeProvider
import com.intellij.bigdatatools.plugin.spark.scala.dfanalysis.impl.DfFileTypeSourceProvider
import com.intellij.bigdatatools.plugin.spark.textIn
import com.intellij.codeInsight.hints.ChangeListener
import com.intellij.codeInsight.hints.ImmediateConfigurable
import com.intellij.codeInsight.hints.InlayHintsCollector
import com.intellij.codeInsight.hints.InlayHintsProvider
import com.intellij.codeInsight.hints.InlayHintsSink
import com.intellij.codeInsight.hints.NoSettings
import com.intellij.codeInsight.hints.SettingsKey
import com.intellij.java.library.JavaLibraryUtil
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiReferenceService
import com.intellij.psi.ResolveResult
import com.intellij.psi.createSmartPointer
import com.intellij.psi.impl.source.tree.LeafPsiElement
import org.intellij.lang.annotations.Language
import org.jetbrains.plugins.scala.lang.psi.api.expr.MethodInvocation
import org.jetbrains.plugins.scala.lang.psi.api.expr.ScExpression
import org.jetbrains.plugins.scala.lang.psi.api.expr.ScMethodCall
import org.jetbrains.plugins.scala.lang.psi.api.expr.ScReferenceExpression
import org.jetbrains.plugins.scala.lang.resolve.ScalaResolveResult
import javax.swing.JPanel

class ScalaDataframeInlayHintsProvider : InlayHintsProvider<NoSettings> {
  override fun getCollectorFor(file: PsiFile,
                               editor: Editor,
                               settings: NoSettings,
                               sink: InlayHintsSink): InlayHintsCollector {
    return ScalaDataframeInlayCollector(editor)
  }

  override fun createSettings() = NoSettings()

  override val name: String
    get() = SAMessagesBundle.message("dataframe.attach.inlay.provider.name")
  override val description: String
    get() = SAMessagesBundle.message("dataframe.attach.inlay.provider.description")
  override val key: SettingsKey<NoSettings>
    get() = SparkAssistanceRegistry.SETTINGS_KEY_SCALA
  override val previewText: String
    @Language("Scala")
    get() = """
      import org.apache.spark.sql.SparkSession

      object ReadParquet extends App {
        val session = SparkSession.builder().getOrCreate()
        session.read.parquet("sample.parquet")
      }
    """.trimIndent()

  override fun createConfigurable(settings: NoSettings): ImmediateConfigurable {
    return object : ImmediateConfigurable {
      override fun createComponent(listener: ChangeListener) = JPanel()
    }
  }

}

class ScalaDataframeInlayCollector(editor: Editor) : DataframeInlayCollector(editor) {
  override fun getReadCallArguments(reference: PsiElement): DataframeInlayPosition? {
    if (reference !is LeafPsiElement) return null
    if (!JavaLibraryUtil.hasLibraryClass(reference.project, "org.apache.spark.sql.DataFrameReader")) return null
    if (!reference.textIn(DataframeSampleFileService.SUPPORTED_READ_METHODS)) return null
    val methodCall = (reference.parent as? ScReferenceExpression)?.parent as? MethodInvocation ?: return null
    val references = PsiReferenceService.getService().getReferences(reference.parent, PsiReferenceService.Hints.NO_HINTS)
    if (!references.any {
        val target = it.resolve()
        target != null && target.containingFile.name.startsWith("DataFrameReader")
      }) return null
    val readerExpr = methodCall.thisExpr().getOrElse<ScExpression> { null } ?: return null
    if (readerExpr is ScMethodCall) {
      val scalaResolveResult: ScalaResolveResult? = readerExpr.target().getOrElse<ScalaResolveResult> { null }
      val resolveResult: ResolveResult? = scalaResolveResult
      val targetElement = resolveResult?.element
      if (targetElement != null && scalaResolveResult?.name() == "schema") return null
    }
    val pathIsConstant = if (methodCall is ScMethodCall) {
      DfAbstractMethodBasedTypeProvider.foldStringParam(methodCall, DfFileTypeSourceProvider.BASE_PARAMETER_NAME) != null
    }
    else false
    return DataframeInlayPosition(
      readerExpr.textRange,
      methodCall.createSmartPointer(),
      SparkStatisticScala,
      pathIsConstant
    )
  }
}