package com.intellij.bigdatatools.plugin.spark.scala.dfanalysis.impl

import com.intellij.bigdatatools.plugin.spark.assistance.dfanalysis.DfTypeSource
import com.intellij.bigdatatools.plugin.spark.assistance.dfanalysis.SimpleTypeSource
import com.intellij.bigdatatools.plugin.spark.scala.SparkScalaMessagesBundle
import com.intellij.bigdatatools.plugin.spark.scala.dfanalysis.CheckingTypeSource
import com.intellij.bigdatatools.plugin.spark.scala.dfanalysis.DfCompletionUtils
import com.intellij.bigdatatools.plugin.spark.scala.dfanalysis.DfTypeByNameUtil
import com.intellij.bigdatatools.plugin.spark.scala.dfanalysis.DfTypeContext
import com.intellij.bigdatatools.plugin.spark.scala.dfanalysis.DfTypeSourceProvider
import com.intellij.codeHighlighting.Pass
import com.intellij.codeHighlighting.TextEditorHighlightingPass
import com.intellij.codeHighlighting.TextEditorHighlightingPassFactory
import com.intellij.codeHighlighting.TextEditorHighlightingPassFactoryRegistrar
import com.intellij.codeHighlighting.TextEditorHighlightingPassRegistrar
import com.intellij.codeInsight.daemon.impl.BackgroundUpdateHighlightersUtil
import com.intellij.codeInsight.daemon.impl.HighlightInfo
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiReference
import org.jetbrains.plugins.scala.lang.psi.api.ScalaElementVisitor
import org.jetbrains.plugins.scala.lang.psi.api.ScalaFile
import org.jetbrains.plugins.scala.lang.psi.api.ScalaPsiElement
import org.jetbrains.plugins.scala.lang.psi.api.base.ScReference
import org.jetbrains.plugins.scala.lang.psi.api.expr.ScAssignment
import org.jetbrains.plugins.scala.lang.psi.api.expr.ScBlock
import org.jetbrains.plugins.scala.lang.psi.api.expr.ScBlockExpr
import org.jetbrains.plugins.scala.lang.psi.api.expr.ScExpression
import org.jetbrains.plugins.scala.lang.psi.api.expr.ScMethodCall
import org.jetbrains.plugins.scala.lang.psi.api.expr.ScPostfixExpr
import org.jetbrains.plugins.scala.lang.psi.api.expr.ScPrefixExpr
import org.jetbrains.plugins.scala.lang.psi.api.expr.ScReferenceExpression
import org.jetbrains.plugins.scala.lang.psi.api.expr.ScReturn
import org.jetbrains.plugins.scala.lang.psi.api.expr.ScTuple
import org.jetbrains.plugins.scala.lang.psi.api.statements.ScFunctionDefinition
import org.jetbrains.plugins.scala.lang.psi.api.statements.ScPatternDefinition
import org.jetbrains.plugins.scala.lang.psi.api.statements.ScVariableDefinition
import org.jetbrains.plugins.scala.lang.psi.api.toplevel.ScNamedElement
import org.jetbrains.plugins.scala.lang.psi.api.toplevel.ScTypedDefinition

/**
 * val a = <df source expression> (1)
 *
 * a.<df transform, ref expr under method call> (2)
 *  .<df transform, ref expr under method call>
 *  ...
 *  .<df transform, ref expr under method call>
 */
class DfScalaHighlightingPass(private val file: ScalaFile, private val editor: Editor) :
  TextEditorHighlightingPass((file as PsiFile).project, editor.document) {

  override fun doCollectInformation(progress: ProgressIndicator) {
    val highlightingInfos = mutableListOf<HighlightInfo>()
    file.acceptScala(DfRecursiveVisitor(highlightingInfos))
    BackgroundUpdateHighlightersUtil.setHighlightersToEditor(
      (file as PsiFile).project,
      file, editor.document,
      0,
      editor.document.textLength,
      highlightingInfos,
      id
    )
  }

  override fun doApplyInformationToEditor() {
  }

  private inner class DfRecursiveVisitor(
    private val highlightingInfos: MutableList<HighlightInfo>,
    private val currentHop: Int = DfComputingUtil.DF_MAX_FILE_HOP_DEPTH
  ) : ScalaElementVisitor() {
    override fun visitScalaElement(s: ScalaPsiElement) {
      (s as PsiElement).children.forEach {
        when (it) {
          is ScalaPsiElement -> it.acceptScala(this)
          else -> it.accept(this)
        }
      }
    }

    override fun visitReference(ref: ScReference) {
      visitScalaElement(ref)
      propagate(ref)
    }

    override fun visitReferenceExpression(ref: ScReferenceExpression) {
      visitScalaElement(ref)
      processExprLike(ref)
    }

    override fun visitAssignment(stmt: ScAssignment) {
      visitScalaElement(stmt)
      propagate(stmt)
    }

    override fun visitPostfixExpression(p: ScPostfixExpr) {
      visitScalaElement(p)
      propagate(p)
    }

    override fun visitBlockExpression(block: ScBlockExpr) {
      super.visitBlockExpression(block)
      propagate(block)
    }

    override fun visitReturn(ret: ScReturn) {
      super.visitReturn(ret)
      propagate(ret)
    }

    override fun visitPrefixExpression(p: ScPrefixExpr) {
      visitScalaElement(p)
      propagate(p)
    }

    override fun visitPatternDefinition(pat: ScPatternDefinition) {
      visitScalaElement(pat)

      val maybeBody = pat.expr()
      if (maybeBody.isEmpty) return

      val body = maybeBody.get()
      val declared = pat.declaredElements()

      fun propagateContext(body: ScalaPsiElement, declaration: ScTypedDefinition) {
        DfComputingUtil.findContext(body)?.let { context ->
          DfComputingUtil.saveToElement(
            DfComputingUtil.COMPUTED_DF_TYPE_KEY,
            SimpleTypeSource(
              context.toSchema(),
              SparkScalaMessagesBundle.message(
                "df.declaration.ts.description",
                DfComputingUtil.trimTextForDescription((declaration as? ScNamedElement)?.name ?: (declaration as PsiElement).text)
              )
            ),
            declaration as PsiElement
          )
        }
      }

      if (declared.length() == 1)
        propagateContext(body, declared.head())
      else
        (body as? ScTuple)?.exprs()?.let { tupleParts ->
          for (i in 0 until declared.size()) {
            val b = tupleParts.apply(i)
            val dc = declared.apply(i)

            propagateContext(b, dc)
          }
        }
    }

    override fun visitVariableDefinition(varr: ScVariableDefinition) {
      visitScalaElement(varr)
      propagate(varr)
    }

    override fun visitExpression(expr: ScExpression) {
      visitScalaElement(expr)
      processExprLike(expr)
    }

    override fun visitTuple(tuple: ScTuple) {
      visitScalaElement(tuple)
      propagate(tuple)
    }

    override fun visitMethodCallExpression(call: ScMethodCall) {
      visitScalaElement(call)
      propagate(call)
    }

    private fun processContext(expr: ScExpression) {
      val context = DfComputingUtil.findContext(expr) ?: return
      val placeDesc = DfComputingUtil.psiElementToDescription(expr as PsiElement)

      DfTypeByNameUtil.getProvider().findTypeChange(expr)?.applyChanges(context, placeDesc)?.let { error ->
        error.logUsage(expr)
        highlightingInfos.addAll(error.produceHighlighting(expr))
      }

      DfComputingUtil.saveToElement(DfComputingUtil.COMMON_DF_MID_TYPE_KEY,
                                    context.toSchema(),
                                    expr as PsiElement)

      propagate(expr)
    }

    private fun processTypeSource(expr: ScExpression) {
      val source = getTypeSource(expr as PsiElement) ?: return
      val context = DfTypeContext(source)

      if (source is CheckingTypeSource) {
        for (error in source.errors) {
          error.logUsage(expr)
          highlightingInfos.addAll(error.produceHighlighting(expr))
        }
      }

      DfComputingUtil.saveToElement(DfComputingUtil.COMMON_DF_TYPE_KEY,
                                    context,
                                    expr as PsiElement)

      DfComputingUtil.saveToElement(DfComputingUtil.COMMON_DF_TYPE_KEY,
                                    context,
                                    (expr as PsiElement).parent)
    }

    private fun processExprLike(expr: ScExpression) {
      processContext(expr)
      processTypeSource(expr)
    }

    /*
    todo: right now we can only propagate one state of the type context, so in cases like

    def createDf() {
      <...>
      if () return DF1
      <...>
      return DF2
    }

    only the last context state will be processed
     */
    private fun propagate(scalaPsi: ScalaPsiElement) {
      val context = DfComputingUtil.findContext(scalaPsi) ?: return
      val parentElement = (scalaPsi as PsiElement).parent ?: return
      if (parentElement !is ScBlock || scalaPsi is ScReturn || parentElement.resultExpression().exists { it == scalaPsi }) {
        DfComputingUtil.saveToElement(DfComputingUtil.COMMON_DF_TYPE_KEY,
                                      context,
                                      parentElement)
      }
    }

    private fun getTypeSource(psiElement: PsiElement): DfTypeSource? {
      fun getOrConvertToTypeContext(e: ScalaPsiElement?): DfTypeContext? =
        if (e == null)
          null
        else
          DfComputingUtil.findContext(e) ?: DfComputingUtil.getFromElement(DfComputingUtil.COMPUTED_DF_TYPE_KEY, e)?.let {
            DfTypeContext(it)
          }

      fun getTypeSourceOuter(call: ScMethodCall): DfTypeSource? {
        if (!DfComputingUtil.isDataFrameType(call)) return null

        val base = (call.deepestInvokedExpr() as? PsiReference)?.resolve() as? ScFunctionDefinition ?: return null
        val fqn = base.containingClass()?.qualifiedName() ?: base.syntheticContainingClass().qualifiedName()
        if (fqn == null || fqn.startsWith(DfComputingUtil.APACHE_SPARK_PACKAGE)) return null

        if (DfComputingUtil.findContext(base) == null && currentHop > 0)
          base.acceptScala(DfRecursiveVisitor(mutableListOf(), currentHop - 1))

        val commonContext = DfComputingUtil.findContext(base)?.let { blockContext ->
          if (!blockContext.isPartial || blockContext.getSourceName().isEmpty()) blockContext
          else {
            val maybeInitialParam = call.matchedParameters().find { it._2().name() == blockContext.getSourceName() }
            val merged = if (maybeInitialParam.isEmpty) null
            else {
              val expr = maybeInitialParam.get()._1()
              val initial =
                getOrConvertToTypeContext(expr) ?: if (expr is PsiReference) expr.resolve()?.let {
                  getOrConvertToTypeContext(it as? ScalaPsiElement)
                }
                else null

              initial?.merge(blockContext)
              initial
            }

            merged ?: blockContext
          }
        }

        return commonContext?.let { DfTypeSourceProvider.createSourceFromTypeContext(it, call) }
      }

      fun getTypeSourceInner(e: PsiElement): DfTypeSource? =
        getOrUpdateByKey(e, DfComputingUtil.COMPUTED_DF_TYPE_KEY) {
          DfTypeSourceProvider.findTypeSource(e)
        }

      // todo handle val ref
      return when (psiElement) {
        is PsiReference ->
          (psiElement.parent as? ScMethodCall)?.let { getTypeSourceOuter(it) } ?: getTypeSourceInner(psiElement)
          ?: psiElement.resolve()?.let { getTypeSourceInner(it) }
        else ->
          getTypeSourceInner(psiElement)
      }?.also {
        DfComputingUtil.saveToElement(DfComputingUtil.COMMON_DF_MID_TYPE_KEY,
                                      it.schema(),
                                      psiElement)
      }
    }

    private fun <T> getOrUpdateByKey(e: PsiElement, key: Key<DfComputingUtil.CachedValue<T>>, update: () -> T?): T? {
      val data = DfComputingUtil.getFromElement(key, e)

      if (data != null)
        return data

      val t = update()
      if (t != null)
        DfComputingUtil.saveToElement(key, t, e)

      return t
    }
  }
}

class DfScalaHighlightingPassFactory : TextEditorHighlightingPassFactory, TextEditorHighlightingPassFactoryRegistrar {
  override fun registerHighlightingPassFactory(registrar: TextEditorHighlightingPassRegistrar, project: Project) {
    if (!DfCompletionUtils.isEnabled()) {
      return
    }

    registrar.registerTextEditorHighlightingPass(this,
                                                 intArrayOf(Pass.UPDATE_ALL),
                                                 null,
                                                 false,
                                                 -1)
  }

  override fun createHighlightingPass(psiFile: PsiFile, editor: Editor): TextEditorHighlightingPass? {
    JavaPsiFacade.getInstance(psiFile.project).findPackage(DfComputingUtil.APACHE_SPARK_PACKAGE) ?: return null

    return (psiFile as? ScalaFile)?.let { DfScalaHighlightingPass(it, editor) }
  }
}