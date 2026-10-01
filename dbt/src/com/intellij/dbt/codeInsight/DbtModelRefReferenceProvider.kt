package com.intellij.dbt.codeInsight

import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.dbt.DbtBundle
import com.intellij.dbt.DbtUtils
import com.intellij.jinja.psi.Jinja2StringLiteral
import com.intellij.jinja.tags.Jinja2FunctionCall
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.util.registry.Registry
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.findDirectory
import com.intellij.openapi.vfs.findPsiFile
import com.intellij.patterns.PatternCondition
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import com.intellij.psi.PsiReferenceProvider
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.ArrayUtilRt
import com.intellij.util.FileSearchUtil
import com.intellij.util.ProcessingContext
import kotlin.math.max
import kotlin.math.min


class DbtModelRefReferenceProvider: PsiReferenceProvider() {
  override fun getReferencesByElement(element: PsiElement, context: ProcessingContext): Array<PsiReference> {
    val module = ModuleUtilCore.findModuleForPsiElement(element.containingFile) ?: return PsiReference.EMPTY_ARRAY
    if (!DbtUtils.isDbtModule(module)) {
      return PsiReference.EMPTY_ARRAY
    }
    val modelNameStringLiteral : Jinja2StringLiteral =
      PsiTreeUtil.findChildOfType(element, Jinja2StringLiteral::class.java) ?: return emptyArray<PsiReference>()
    return arrayOf(DbtModelRefReference(element as Jinja2FunctionCall, modelNameStringLiteral))
  }

  class DbtModelRefReference(element: Jinja2FunctionCall, private val modelNameStringLiteral : Jinja2StringLiteral)
    : DbtModelReferenceBase<Jinja2FunctionCall>(element, getValueRangeInStringLiteral(modelNameStringLiteral.getTextLength())) {
    override fun isSoft(): Boolean = true

    private fun findFileInDirectory(dir: VirtualFile?, referenceModelFileName: String) =
      FileSearchUtil
        .findFileRecursively(dir, referenceModelFileName, Registry.intValue("dbt.models.max.depth.level", 10), 1000L)
        .findFirst()

    override fun resolveInner(): PsiElement? {
      val referenceModelFileName = "${modelNameStringLiteral.getStringValue()}.sql"
      val referenceSeedModelFileName = "${modelNameStringLiteral.getStringValue()}.csv"

      val module = ModuleUtilCore.findModuleForPsiElement(element) ?: return null
      val dbtDirectory = DbtUtils.getDbtDirectory(module) ?: return null

      val modelsDir = dbtDirectory.findDirectory(MODELS_DIR)
      val seedsDir = dbtDirectory.findDirectory(SEEDS_DIR)

      val referenceModelFile = findFileInDirectory(modelsDir, referenceModelFileName) ?:
                               findFileInDirectory(seedsDir, referenceSeedModelFileName) ?: return null

      return referenceModelFile.findPsiFile(element.project)
    }

    override fun getVariants(): Array<Any> {
      val module = ModuleUtilCore.findModuleForPsiElement(element) ?: return ArrayUtilRt.EMPTY_OBJECT_ARRAY

      val allModelExceptCurrentFile = DbtUtils.getAllModels(module).filter { it != element.containingFile.originalFile.virtualFile }.map{it.nameWithoutExtension}.map {
        LookupElementBuilder.create(it).withTypeText(DbtBundle.message("dbt.completion.model.suffix"))
      }
      val seedLookupItems = DbtUtils.getAllSeeds(module).map{it.nameWithoutExtension}.map {
        LookupElementBuilder.create(it).withTypeText(DbtBundle.message("dbt.completion.seed.suffix"))
      }

      return (allModelExceptCurrentFile + seedLookupItems).toTypedArray()
    }
  }

  companion object {
    val isReferenceFunction = object : PatternCondition<Jinja2FunctionCall>("referenceJinjaFunction") {
      override fun accepts(expression: Jinja2FunctionCall, context: ProcessingContext?): Boolean {
        return expression.callee?.text == REF_FUNCTION_NAME
      }
    }

    private fun getValueRangeInStringLiteral(length: Int) = TextRange.from(min(REF_FUNCTION_NAME.length + 2, length - 1), max(0, length - 2))

    private const val REF_FUNCTION_NAME = "ref"
    // TODO: models and seeds dirs must be received from dbt_project.yml
    private const val MODELS_DIR = "models"
    private const val SEEDS_DIR = "seeds"
  }
}