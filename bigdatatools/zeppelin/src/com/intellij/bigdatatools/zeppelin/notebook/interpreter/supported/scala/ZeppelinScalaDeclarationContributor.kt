package com.intellij.bigdatatools.zeppelin.notebook.interpreter.supported.scala

import com.intellij.bigdatatools.notebooks.core.impl.file.NotebookVirtualFile
import com.intellij.bigdatatools.zeppelin.drivers.ZeppelinDriverManager
import com.intellij.bigdatatools.zeppelin.interpreters.components.SparkInterpreterPrecodeHandler
import com.intellij.bigdatatools.zeppelin.language.ZeppelinLanguage
import com.intellij.bigdatatools.zeppelin.models.SparkVersion
import com.intellij.bigdatatools.zeppelin.notebook.parser.ZeppelinFileViewProvider
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.ResolveState
import com.intellij.psi.scope.PsiScopeProcessor
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.plugins.scala.lang.psi.api.FileDeclarationsContributor
import org.jetbrains.plugins.scala.lang.psi.api.ScalaFile
import org.jetbrains.plugins.scala.lang.psi.impl.ScalaPsiElementFactory
import org.jetbrains.plugins.scala.lang.psi.impl.toplevel.templates.ScTemplateBodyImpl
import org.jetbrains.plugins.scala.lang.psi.impl.toplevel.typedef.ScClassImpl
import org.jetbrains.plugins.scala.project.ScalaFeatures

/**
 * Add to IDEA special Zeppelin imports and resolve Zeppelin built-in variables (sc, sqlc, etc)
 */
class ZeppelinScalaDeclarationContributor : FileDeclarationsContributor() {
  private val recGuard = ThreadLocal<String>()

  override fun accept(holder: PsiElement): Boolean {
    // so we don't get exceptions because of empty dummy.scala from empty precode
    if (holder is PsiFile && holder.viewProvider !is ZeppelinFileViewProvider || holder.firstChild == null) return false

    val containingFile = holder.firstChild.containingFile
    val originalFile: PsiFile = containingFile.originalFile
    return originalFile is ScalaFile && originalFile.viewProvider.baseLanguage is ZeppelinLanguage
  }

  override fun processAdditionalDeclarations(processor: PsiScopeProcessor?,
                                             holder: PsiElement?,
                                             state: ResolveState?,
                                             lastParent: PsiElement?) {
    if ((recGuard.get() != null && holder?.getCopyableUserData(RESOLVE_IN_PROCESS) != null) || holder !is ScalaFile) return
    if (processor == null || state == null) return

    try {
      recGuard.set("true")
      holder.putCopyableUserData(RESOLVE_IN_PROCESS, "true")
      val config = ZeppelinDriverManager.getDrivers(holder.project).firstOrNull()?.connectionData ?: return

      val version = SparkVersion(config.sparkVersion)
      proceedBuiltins(version, holder, processor, state)
      proceedGlobalImports(version, holder, processor, state)
      proceedContextImplicits(version, holder, processor, holder, state)
      val scalaFeatures = ScalaFeatures.forPsiOrDefault(holder as PsiElement)
      processPrecode(holder as PsiFile, processor, state, scalaFeatures)
    }
    finally {
      recGuard.remove()
      holder.putCopyableUserData(RESOLVE_IN_PROCESS, null)
    }
  }

  private fun processPrecode(holder: PsiFile, processor: PsiScopeProcessor, state: ResolveState, scalaFeatures: ScalaFeatures) {
    val code = SparkInterpreterPrecodeHandler.getForFile(holder.virtualFile as? NotebookVirtualFile ?: return)?.sparkPrecode ?: return
    val fileText = "class Precode {\n  $code \n}"
    val scDef = ScalaPsiElementFactory.createScalaFileFromText(fileText, scalaFeatures, false, true, true, holder.project).typeDefinitions().head()

    val templateBodyOpt = (scDef as ScClassImpl).extendsBlock().templateBody()
    if (templateBodyOpt.isEmpty) return
    val templateBody = (templateBodyOpt.get() as? ScTemplateBodyImpl) ?: return

    (templateBody as PsiElement).processDeclarations(processor, state, null, holder)
    templateBody.importStatements.foreach {
      (it as PsiElement).processDeclarations(processor, state, null, holder)
    }
  }

  private fun proceedBuiltins(version: SparkVersion,
                              zeppelinFile: ScalaFile,
                              processor: PsiScopeProcessor,
                              state: ResolveState) {
    val project: Project = (zeppelinFile as PsiFile).project
    val scalaFeatures = ScalaFeatures.forPsiOrDefault(zeppelinFile as PsiElement)
    val builtins: List<PsiElement> = getBuiltins(version, project, scalaFeatures)
    builtins.forEach { it.processDeclarations(processor, state, null, zeppelinFile) }
  }

  private fun proceedContextImplicits(version: SparkVersion, zeppelinFile: ScalaFile, processor: PsiScopeProcessor,
                                      holder: PsiElement, state: ResolveState) {
    val contextImplicits: List<String> = if (version.isSpark2()) {
      listOf("org.apache.spark.sql.SparkSession")
    }
    else {
      listOf("org.apache.spark.sql.SQLContext")
    }
    contextImplicits.forEach {
      val psiClass = JavaPsiFacade.getInstance(holder.project).findClass(it,
                                                                         GlobalSearchScope.allScope(holder.project)) ?: return
      val list: List<PsiClass> = psiClass.innerClasses.toList()
      list.first().processDeclarations(processor, state, null, zeppelinFile)
    }
  }

  private fun proceedGlobalImports(version: SparkVersion,
                                   zeppelinFile: ScalaFile,
                                   processor: PsiScopeProcessor,
                                   state: ResolveState) {
    val globalImports: List<PsiElement> = getGlobalImports(version, zeppelinFile)
    globalImports.forEach { it.processDeclarations(processor, state, null, zeppelinFile) }
  }

  companion object {
    private val RESOLVE_IN_PROCESS = Key<String>("ZeppelinScalaResolveInProcess")

    private val DEFAULT_GLOBAL_IMPORTS: List<String> = listOf("org.apache.spark.SparkContext._")
    private val FUNCTIONS_IMPORTS: List<String> = listOf("org.apache.spark.sql.functions._")
    private val DEFAULT_BUILTINS: List<Pair<String, String>> = listOf(
      Pair("z", "org.apache.zeppelin.spark.SparkZeppelinContext"),
      Pair("sc", "org.apache.spark.SparkContext"),
      Pair("sqlContext", "org.apache.spark.sql.SQLContext"),
      Pair("$" + "intp", "scala.tools.nsc.interpreter.IMain"),
      Pair("sqlc", "org.apache.spark.sql.SQLContext"),
      Pair("senv", "org.apache.flink.streaming.api.environment.StreamExecutionEnvironment"),
      Pair("benv", "org.apache.flink.api.java.ExecutionEnvironment"),
      Pair("stenv", "org.apache.flink.table.api.bridge.java.StreamTableEnvironment"),
      Pair("btenv", "org.apache.flink.table.api.java.BatchTableEnvironment"),
      Pair("stenv_2", "org.apache.flink.table.api.bridge.java.StreamTableEnvironment"),
      Pair("btenv_2", "org.apache.flink.table.api.java.BatchTableEnvironment"),
      Pair("lastException", "java.lang.Exception")
    )
    private val SPARK_2_BULTINS: List<Pair<String, String>> = listOf(
      Pair("spark", "org.apache.spark.sql.SparkSession"))
    private var builtinsMap: MutableMap<Pair<String, Project>, List<PsiElement>> = mutableMapOf()
    private var globalImportMap: MutableMap<Pair<String, Project>, List<PsiElement>> = mutableMapOf()



    private fun getBuiltins(version: SparkVersion, project: Project, scalaFeatures: ScalaFeatures): List<PsiElement> {
      val keyPair = Pair(version.versionString, project)
      val builtin = builtinsMap[keyPair]
      if (builtin != null) {
        if (builtin.all {  ModuleUtilCore.findModuleForPsiElement(it)?.isDisposed == false }) return builtin
        builtinsMap.remove(keyPair)
      }
      val computedBuiltins: List<PsiElement> = getInnerBuiltins(version, project, scalaFeatures)
      builtinsMap = mutableMapOf(keyPair to computedBuiltins)
      return computedBuiltins
    }

    private fun getGlobalImports(version: SparkVersion, context: PsiElement): List<PsiElement> {
      val keyPair = Pair(version.versionString, context.project)
      val imports: List<PsiElement>? = globalImportMap[keyPair]
      if (imports != null) {
        if (imports.all { ModuleUtilCore.findModuleForPsiElement(it)?.isDisposed == false }) return imports
        globalImportMap.remove(keyPair)
      }
      val computedGlobalImports: List<PsiElement> = getInnerGlobalImports(version, context)
      globalImportMap = mutableMapOf(keyPair to computedGlobalImports)
      return computedGlobalImports
    }

    private fun getInnerBuiltins(version: SparkVersion, project: Project, scalaFeatures: ScalaFeatures): List<PsiElement> {
      val builtinsNames = if (version.isSpark2()) {
        DEFAULT_BUILTINS + SPARK_2_BULTINS
      }
      else {
        DEFAULT_BUILTINS
      }
      return builtinsNames.map {
        val name: String = it.first
        val txt: String = it.second
        ScalaPsiElementFactory.createElementFromText("class Builtin { val $name:$txt = ??? }", scalaFeatures, project)
      }
    }

    private fun getInnerGlobalImports(version: SparkVersion, context: PsiElement): List<PsiElement> {
      val globalImports: List<String> = if (!version.oldSqlContextImplicits()) {
        DEFAULT_GLOBAL_IMPORTS + FUNCTIONS_IMPORTS
      }
      else {
        DEFAULT_GLOBAL_IMPORTS
      }
      return globalImports.map {
        ScalaPsiElementFactory.createImportFromText("import $it", context, null)
      }
    }
  }
}