package com.intellij.bigdatatools.zeppelin.refactoring

import com.intellij.bigdatatools.zeppelin.notebook.parser.ZeppelinFileViewProvider
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.actionSystem.LangDataKeys
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.*
import com.intellij.psi.codeStyle.CodeStyleManager
import com.intellij.psi.util.parents
import com.intellij.psi.util.siblings
import com.jetbrains.bigdatatools.common.util.invokeLater
import org.jetbrains.plugins.scala.ScalaLanguage
import org.jetbrains.plugins.scala.editor.importOptimizer.ScalaImportOptimizer
import org.jetbrains.plugins.scala.lang.psi.api.ScalaFile
import org.jetbrains.plugins.scala.lang.psi.api.ScalaPsiElement
import org.jetbrains.plugins.scala.lang.psi.api.ScalaRecursiveElementVisitor
import org.jetbrains.plugins.scala.lang.psi.api.base.ScReference
import org.jetbrains.plugins.scala.lang.psi.api.expr.ScTuple
import org.jetbrains.plugins.scala.lang.psi.api.statements.ScFunction
import org.jetbrains.plugins.scala.lang.psi.api.statements.ScPatternDefinition
import org.jetbrains.plugins.scala.lang.psi.api.statements.ScValueOrVariable
import org.jetbrains.plugins.scala.lang.psi.api.toplevel.imports.ScImportStmt
import org.jetbrains.plugins.scala.lang.psi.api.toplevel.typedef.ScMember
import org.jetbrains.plugins.scala.lang.psi.api.toplevel.typedef.ScTemplateDefinition
import org.jetbrains.plugins.scala.lang.psi.impl.ScalaPsiElementFactory
import org.jetbrains.plugins.scala.lang.psi.types.Context
import org.jetbrains.plugins.scala.lang.psi.types.TypePresentationContext
import org.jetbrains.plugins.scala.lang.psi.types.api.ParameterizedType
import org.jetbrains.plugins.scala.project.ScalaFeatures
import java.util.*

object ZeppelinExtractRefactoringUtil {
  data class ExtractedJobInfo(val importsBlock: String,
                              val codeBlock: String,
                              val defaultVarsUsed: List<OuterVariableInfo>,
                              val outerVarsUsed: List<OuterVariableInfo>)

  data class OuterVariableInfo(val name: String,
                               val tpe: String,
                               val generatedCodeIdx: Int,
                               val outerBlock: String = "",
                               val boundImportsBlock: String = "")

  val SUPPORTED_CODES = hashSetOf("", "spark", "livy")

  private val EMPTY_JOB_INFO = ExtractedJobInfo("", "", emptyList(), emptyList())

  fun isActionAccessible(e: AnActionEvent): Boolean {
    return true == (e.getData(CommonDataKeys.PSI_FILE)?.viewProvider as? ZeppelinFileViewProvider)?.let { provider ->
      (provider.getPsi(ScalaLanguage.INSTANCE) as? ScalaFile)?.let { scalaFile ->
        e.getData(CommonDataKeys.EDITOR)?.let { editor ->
          ZeppelinExtractHandlerBase.guessSelection(scalaFile, editor).isNotEmpty()
        }
      }
    }
  }

  fun extractJobInfo(baseFile: PsiFile, selectedElements: List<PsiElement>): ExtractedJobInfo {
    val (dummyFile, payloadStart, signatureEnd) = createDummyHolder(baseFile, selectedElements)
    return processDummyHolder(dummyFile, payloadStart, signatureEnd)
  }

  fun createMethodText(fileText: ExtractedJobInfo, outerNamesInSignature: Set<String>, name: String, visibility: String): String {
    val signatureBlock = StringBuilder()
    val nonSignatureBlock = StringBuilder()
    val boundSignatureImports = StringBuilder()
    val codeBlock = fileText.codeBlock
    var lastCodePosition = 0

    for (v in fileText.defaultVarsUsed + fileText.outerVarsUsed) {
      if (outerNamesInSignature.contains(v.name)) {
        signatureBlock.append("${v.name}: ${v.tpe},")
        boundSignatureImports.appendLine(v.boundImportsBlock)
      }
      else {
        val codePosition = v.generatedCodeIdx
        nonSignatureBlock.append(codeBlock.subSequence(lastCodePosition, codePosition))
        lastCodePosition = codePosition
        nonSignatureBlock.appendLine(v.outerBlock)
        nonSignatureBlock.appendLine(v.boundImportsBlock)
      }
    }

    if (lastCodePosition < codeBlock.length - 1)
      nonSignatureBlock.append(codeBlock.subSequence(lastCodePosition, codeBlock.length))

    if (signatureBlock.isNotEmpty()) signatureBlock.deleteCharAt(signatureBlock.length - 1)

    return """
      |${visibility.trim()} def $name($signatureBlock) {
      |  $boundSignatureImports
      |  $nonSignatureBlock
      |}
    """.trimMargin()
  }

  fun getTopLevelElement(el: PsiElement): PsiElement {
    if (el.parent is PsiFile) return el

    val parents = el.parents(false).toList()
    return if (parents.size < 2) el else parents[parents.size - 2]
  }

  fun postProcessFile(virtualFile: VirtualFile, psiFile: PsiFile?, project: Project,
                      dataContext: DataContext?, withImportOptimizer: Boolean = true) {
    invokeLater {
      FileEditorManager.getInstance(project).openFile(virtualFile, true)

      dataContext?.let { context ->
        (psiFile ?: PsiManager.getInstance(project).findFile(virtualFile))?.viewProvider?.getPsi(ScalaLanguage.INSTANCE)?.let { pFile ->
          val viewProvider = pFile.viewProvider
          LangDataKeys.IDE_VIEW.getData(context)?.selectElement(viewProvider.getPsi(viewProvider.baseLanguage)!!)
          WriteCommandAction.runWriteCommandAction(
            project, null, null, Runnable { CodeStyleManager.getInstance(project).reformat(pFile) }, pFile
          )
          if (withImportOptimizer) runScalaImportOptimizer(pFile)
        }
      }
    }
  }

  fun VirtualFile.toPsiDirectory(project: Project): PsiDirectory? = PsiManager.getInstance(project).findDirectory(this)

  private fun <T> scala.Option<T>.getOrNull(): T? = if (isDefined) get() else null

  private fun runScalaImportOptimizer(psiFile: PsiFile) {
    (psiFile as? ScalaFile)?.let { scalaFile ->
      val maybeOptimizer = ScalaImportOptimizer.findOptimizerFor(scalaFile)
      if (maybeOptimizer.isDefined) (maybeOptimizer.get() as? ScalaImportOptimizer)?.processFile(psiFile)?.let { runnable ->
        WriteCommandAction.runWriteCommandAction(psiFile.project, null, null, runnable, psiFile)
      }
    }
  }

  private fun createDummyHolder(baseFile: PsiFile, selectedElements: List<PsiElement>): MarkedDummyFile {
    val scalaFile = baseFile.viewProvider.getPsi(ScalaLanguage.INSTANCE)!!
    val prologue = getDefaultImports() + "\n\n" + getDefaultDeclarations() + "\n\n"
    val result = StringBuilder(prologue)
    val selectionStart = if (selectedElements.isEmpty()) 0 else selectedElements.first().textOffset

    for (child in scalaFile.children.takeWhile { it.textRange.endOffset <= selectionStart }) {
      if (child is ScTemplateDefinition || child is ScValueOrVariable || child is ScFunction || child is ScImportStmt)
        result.append(child.text).append("\n\n")
    }

    val payloadStart = result.length

    selectedElements.map { getTopLevelElement(it) }.toSet().sortedBy { it.textRange.startOffset }.forEach { result.appendLine(it.text) }

    val scalaFeatures = ScalaFeatures.forPsiOrDefault(scalaFile)
    return MarkedDummyFile(createDummyScalaFile(result.toString(), scalaFeatures, baseFile.project), payloadStart, prologue.length)
  }

  private fun processDummyHolder(dummyFile: ScalaFile, payloadStart: Int, signatureEnd: Int): ExtractedJobInfo {
    val firstRoot = getTopLevelElement((dummyFile as PsiFile).findElementAt(payloadStart) ?: return EMPTY_JOB_INFO)

    val roots = ArrayDeque<PsiElement>()
    roots.add(firstRoot)
    val output = LinkedHashSet<PsiElement>()
    val importsUsed = mutableSetOf<PsiElement>()
    val boundImportsMap = hashMapOf<PsiElement, HashSet<String>>()

    for (sibling in firstRoot.siblings()) if (sibling !is PsiWhiteSpace) roots.addLast(sibling)

    fun checkFile(psiElement: PsiElement): Boolean =
      psiElement.containingFile != null && psiElement.containingFile.viewProvider == dummyFile.viewProvider

    class ZeppelinMarkingVisitor : ScalaRecursiveElementVisitor() {
      override fun visitReference(ref: ScReference?) {
        val resolveResult = ref?.multiResolveScala(false)

        resolveResult?.let { rr ->
          rr.forEach { result ->
            val impUsed = result.importsUsed().iterator()
            @Suppress("KotlinConstantConditions")
            while (impUsed.hasNext()) {
              val used = impUsed.next()

              var firstQualifier = used.importExpr().getOrNull()?.qualifier()?.getOrNull()
              while (firstQualifier != null && firstQualifier.qualifier().isDefined) firstQualifier = firstQualifier.qualifier()?.getOrNull()
              val resolvedElement = firstQualifier?.element?.multiResolveScala(false)?.firstOrNull()?.element as? PsiElement

              if (resolvedElement != null && checkFile(resolvedElement)) {
                val topLevelElement = getTopLevelElement(resolvedElement)
                val elementText = "import " + (used.element() as PsiElement).text
                val set = boundImportsMap.getOrPut(topLevelElement) { hashSetOf() }

                if (!set.contains(elementText)) {
                  set.add(elementText)
                  if (!roots.contains(topLevelElement))
                    roots.add(topLevelElement)
                }
              }
              else importsUsed.add((used.element() as PsiElement).parent)
            }

            result.element?.let { element: PsiElement ->
              val teElement = getTopLevelElement(element)
              val actualElement =
                if (checkFile(teElement)) teElement
                else if (null != (teElement as? ScMember)?.syntheticNavigationElement()) teElement.containingClass()
                else null

              if (actualElement != null && !roots.contains(actualElement) && !output.contains(actualElement))
                if (!roots.contains(teElement)) {
                  roots.addLast(teElement)
                }
            }
          }
        }

        (ref as? PsiElement)?.children?.forEach { (it as? ScalaPsiElement)?.acceptScala(this) }
      }
    }

    val processedRoots = mutableSetOf<PsiElement>()
    while (roots.isNotEmpty()) {
      val r = roots.removeFirst()
      if (!processedRoots.contains(r)) {
        processedRoots.add(r)
        r.accept(ZeppelinMarkingVisitor())
      }
      output.add(r)
    }

    val codeBlock = StringBuilder()
    val importsBlock = StringBuilder()

    val defaultVariablesUsed = mutableListOf<OuterVariableInfo>()
    val outerVariables = mutableListOf<OuterVariableInfo>()

    fun handleOuterVariable(definition: ScPatternDefinition, acc: MutableList<OuterVariableInfo>) {
      val generatedIdx = codeBlock.length
      val tpeContext = TypePresentationContext.emptyContext()
      val context = Context.apply(definition)
      val maybeTpe = definition.type()

      val boundImportsBlock = boundImportsMap[definition]?.joinToString(separator = "\n") ?: ""

      if (maybeTpe.isLeft) {
        acc.add(OuterVariableInfo(
          definition.declaredNames().head(),
          "Any",
          generatedIdx,
          (definition as PsiElement).text,
          boundImportsBlock
        ))
        return
      }

      val tpe = maybeTpe.right().get()

      if (tpe.typeDepth() > 1 && tpe is ParameterizedType && tpe.designator().canonicalText().contains("scala.Tuple")) {
        if (definition.expr().isEmpty || definition.expr().get() !is ScTuple) {
          codeBlock.append((definition as PsiElement).text).append("\n\n")
          return
        }

        val i1 = definition.declaredNames().iterator()
        val i2 = tpe.typeArguments().iterator()
        val i3 = (definition.expr().get() as ScTuple).exprs().iterator()

        (definition.expr().get() as ScTuple).exprs()

        while (i1.hasNext() && i2.hasNext() && i3.hasNext()) {
          val patternPart = i1.next()

          acc.add(OuterVariableInfo(
            patternPart,
            i2.next().presentableText(tpeContext, context),
            generatedIdx,
            "val $patternPart = ${(i3.next() as PsiElement).text}\n",
            boundImportsBlock
          ))
        }
        return
      }

      acc.add(OuterVariableInfo(
        definition.declaredNames().head(),
        tpe.presentableText(tpeContext, context),
        generatedIdx,
        (definition as PsiElement).text,
        boundImportsBlock
      ))
    }

    for (child in output.first().containingFile.children.takeWhile { it.textRange.endOffset < payloadStart }) when {
      importsUsed.contains(child) -> importsBlock.append(child.text).append("\n")
      output.contains(child) && child.textRange.endOffset < signatureEnd -> (child as? ScPatternDefinition)?.let { patternDefinition ->
        handleOuterVariable(patternDefinition, defaultVariablesUsed)
      }
      output.contains(child) ->
        if (child is ScPatternDefinition) handleOuterVariable(child, outerVariables) else codeBlock.append(child.text).append("\n\n")
    }

    codeBlock.append(dummyFile.text.substring(payloadStart))

    return ExtractedJobInfo(importsBlock.toString(), codeBlock.toString(), defaultVariablesUsed, outerVariables)
  }

  private fun createDummyScalaFile(text: String, scalaFeatures: ScalaFeatures, project: Project): ScalaFile =
    ScalaPsiElementFactory.createScalaFileFromText(text, scalaFeatures, false, true, true, project)

  private data class MarkedDummyFile(val dummyFile: ScalaFile, val payloadStart: Int, val signatureEnd: Int)

  private fun getDefaultImports(): String = listOf(
    "org.apache.spark.SparkContext._", "org.apache.zeppelin.spark.SparkZeppelinContext", "org.apache.spark.SparkContext",
    "org.apache.spark.sql.SQLContext", "org.apache.spark.sql.SQLContext", "org.apache.spark.sql.SparkSession",
    "org.apache.spark.sql.functions._", "org.apache.spark.implicits._"
  ).joinToString("\n\n") { "import $it" }

  private fun getDefaultDeclarations(): String = listOf(
    "z: SparkZeppelinContext", "sc: SparkContext", "sqlContext: SQLContext", "sqlc: SQLContext", "spark: SparkSession"
  ).joinToString("\n\n") { "val $it = ???" }
}