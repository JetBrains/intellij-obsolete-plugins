package com.intellij.dbt

import com.intellij.database.dialects.base.startOffset
import com.intellij.dbt.console.getDbtExecutableName
import com.intellij.execution.configurations.PathEnvironmentVariableUtil
import com.intellij.execution.configurations.PtyCommandLine
import com.intellij.jinja.psi.Jinja2StringLiteral
import com.intellij.jinja.tags.Jinja2FunctionCall
import com.intellij.jinja.template.parsing.DjangoTemplateTokenTypes
import com.intellij.jinja.template.psi.impl.DjangoTagElementImpl
import com.intellij.jinja.template.psi.impl.Jinja2VariableReferenceImpl
import com.intellij.lang.Language
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.project.guessModuleDir
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.project.rootManager
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.SystemInfo
import com.intellij.openapi.util.io.OSAgnosticPathUtil
import com.intellij.openapi.vfs.StandardFileSystems
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.ex.temp.TempFileSystem
import com.intellij.openapi.vfs.findFile
import com.intellij.openapi.vfs.isFile
import com.intellij.platform.backend.workspace.virtualFile
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.psi.search.FilenameIndex
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.templateLanguages.TemplateLanguageFileViewProvider
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.elementType
import com.intellij.sql.SqlFileType
import com.intellij.sql.dialects.SqlLanguageDialect
import com.intellij.sql.psi.SqlLanguage
import com.intellij.sql.psi.SqlQueryExpression
import com.intellij.workspaceModel.ide.impl.legacyBridge.module.findModuleEntity
import com.intellij.workspaceModel.ide.legacyBridge.ModuleBridge
import org.jetbrains.yaml.psi.YAMLFile
import org.jetbrains.yaml.psi.YAMLMapping
import org.jetbrains.yaml.psi.YAMLScalar
import org.jetbrains.yaml.psi.impl.YAMLArrayImpl
import java.io.File
import java.io.File.separatorChar

class DbtUtils {
  companion object {
    private const val DBT_DIRECTORY_MAX_DEEP_LEVEL = 5
    const val DBT_PROJECT_FILE_NAME = "dbt_project.yml"
    const val DBT_SCHEMA_YML_FILE_NAME = "schema.yml"
    private val DIRECTORIES_TO_IGNORE = arrayOf("dbt_packages", ".idea")
    private const val DBT_DIRECTORIES_TESTS = "tests"
    private const val DBT_DIRECTORIES_MODELS = "models"

    private val DBT_SQL_PATH_KEYS = setOf("model-paths", "analysis-paths", "test-paths", "macro-paths")
    private val DBT_PATHS_CACHE_KEY = Key.create<Pair<Long, List<String>>>("dbt.sql.paths.cache")

    private val SEEDS_FILE_EXTENSION = "csv"

    fun isDbtModule(module : Module?) : Boolean {
      if (module == null) return false
      return getDbtDirectory(module) != null
    }

    fun getDbtDirectory(module: Module): VirtualFile? {
      val moduleBridge = module as? ModuleBridge ?: return null

      val moduleEntity = moduleBridge.findModuleEntity(moduleBridge.entityStorage.current) ?: return null
      return moduleEntity.dbtSettings?.dbtProjectPath?.virtualFile
    }

    fun containsDbtProjectFile(directory: VirtualFile) = directory.findFile(DBT_PROJECT_FILE_NAME) != null

    fun generateDbtInitCommandLine(pathToDbt: String, projectPath: String, name: String, initProfileName: String?, initProfileDir: String) =
      PtyCommandLine()
        .withWorkDirectory(projectPath)
        .withParameters(
          buildList {
            add("init")
            add(name)
            add(SKIP_SETUP_KEY)
            if (initProfileName?.isNotEmpty() == true) {
              add(PROFILE_KEY)
              add(initProfileName)
            }
            if (initProfileDir.isNotEmpty()) {
              add(PROFILES_DIR_KEY)
              add(initProfileDir)
            }
          }
        )
        .withExePath(pathToDbt)

    fun guessDbtExecutable(module: Module): String {
      val moduleDir = module.guessModuleDir()
      val dbtExecutableName = getDbtExecutableName()
      if (moduleDir == null) {
        return dbtExecutableName
      }
      val venvDbtRelativePath = if (SystemInfo.isWindows) "venv/Scripts/$dbtExecutableName" else "venv/bin/$dbtExecutableName"
      val dbtExecutable = moduleDir.findFile(venvDbtRelativePath)
      if (dbtExecutable != null && dbtExecutable.exists()) {
        val dbtFile = File(dbtExecutable.path)
        if (dbtExecutable.fileSystem is TempFileSystem) {
          return dbtFile.path
        }
        if (dbtFile.canExecute()) {
          return dbtFile.path
        }
      }

      val dbtFile = PathEnvironmentVariableUtil.findFirst(dbtExecutableName)
      if (dbtFile != null) {
        return dbtFile.toString()
      }
      return dbtExecutableName
    }

    fun getDbtSettings(module: Module): DbtModuleEntity? {
      val moduleBridge = module as? ModuleBridge ?: return null
      val moduleEntity = moduleBridge.findModuleEntity(moduleBridge.entityStorage.current) ?: return null
      return moduleEntity.dbtSettings
    }

    fun getProfileNamesFromDir(profilesDir: String): List<String> {
      if (profilesDir.isEmpty()) return emptyList()
      val profilesFile = File(profilesDir, "profiles.yml")
      if (!profilesFile.isFile) return emptyList()
      val profilesVirtualFile = StandardFileSystems.local().findFileByPath(profilesFile.path) ?: return emptyList()
      val profilePsiFile = PsiManager.getInstance(ProjectManager.getInstance().defaultProject).findFile(profilesVirtualFile)
      val yamlFile: YAMLFile = profilePsiFile as? YAMLFile ?: return emptyList()
      return (yamlFile.documents.firstOrNull()?.firstChild as? YAMLMapping)?.keyValues?.mapNotNull { it.key?.text }
             ?: return emptyList()
    }

    fun getDbtProfilesDir(dbtDir: File = File(OSAgnosticPathUtil.expandUserHome("~/.dbt"))): String =
      if (dbtDir.isDirectory and File(dbtDir, "profiles.yml").isFile) dbtDir.path else ""

    /**
     * Search dbt project inside the module.
     */
    fun findDbtDirectory(module: Module): VirtualFile? {
      val moduleDir = module.guessModuleDir() ?: return null
      val setOfModuleDir = setOf(moduleDir)
      val candidates = FilenameIndex.getVirtualFilesByName(DBT_PROJECT_FILE_NAME, GlobalSearchScope.projectScope(module.project)).filter{it.isFile && VfsUtil.isUnder(it, setOfModuleDir)}
      if (candidates.isEmpty()) {
        return null
      }
      // return the topmost file
      return candidates.minBy {it.path.count {it == separatorChar}}.parent
    }

    fun isUnderModelsDirectory(virtualFile: VirtualFile, module: Module): Boolean {
      val firstLevelDbtSubfolder = getFirstLevelDbtSubdirectory(virtualFile, module)
      return firstLevelDbtSubfolder?.name == DBT_DIRECTORIES_MODELS
    }

    fun isUnderTestDirectory(virtualFile: VirtualFile, module: Module): Boolean {
      val firstLevelDbtSubfolder = getFirstLevelDbtSubdirectory(virtualFile, module)
      return firstLevelDbtSubfolder?.name == DBT_DIRECTORIES_TESTS
    }

    private fun getFirstLevelDbtSubdirectory(file: VirtualFile, module: Module): VirtualFile? {
      val dbtSettings = getDbtSettings(module) ?: return null
      val dbtRoot = dbtSettings.dbtProjectPath?.virtualFile ?: return null
      if (!VfsUtil.isUnder(file, setOf(dbtRoot))) {
        return null
      }
      var targetFile = file
      @Suppress("SENSELESS_COMPARISON")
      while (targetFile != null && targetFile.parent != dbtRoot) {
        targetFile = targetFile.parent
      }
      return targetFile
    }

    fun getAllModels(module: Module): List<VirtualFile> {
      val dbtDirectory = getDbtDirectory(module) ?: return emptyList()
      val dbtModelsDirectory = dbtDirectory.findChild("models") ?: return emptyList()
      if (dbtModelsDirectory.isFile) {
        return emptyList()
      }

      return VfsUtil.collectChildrenRecursively(dbtModelsDirectory).filter { it.isFile && it.extension == SqlFileType.DEFAULT_EXTENSION}
    }

    fun getAllSeeds(module: Module): List<VirtualFile> {
      val dbtDirectory = getDbtDirectory(module) ?: return emptyList()
      val dbtModelsDirectory = dbtDirectory.findChild("seeds") ?: return emptyList()
      if (dbtModelsDirectory.isFile) {
        return emptyList()
      }

      return VfsUtil.collectChildrenRecursively(dbtModelsDirectory).filter { it.isFile && it.extension == SEEDS_FILE_EXTENSION}
    }

    fun isRefCall(call: Jinja2FunctionCall): Boolean {
      return (call.callee as? Jinja2VariableReferenceImpl)?.name == "ref"
    }

    fun getJinjaCall(element: PsiElement): Jinja2FunctionCall? {
      val viewProvider = element.containingFile.viewProvider
      val jinjaElement = viewProvider.getPsi(viewProvider.baseLanguage)!!.findElementAt(element.startOffset)

      if (jinjaElement.elementType != DjangoTemplateTokenTypes.DJANGO_EXPRESSION_START) {
        return null
      }

      val tag = PsiTreeUtil.getParentOfType(jinjaElement, DjangoTagElementImpl::class.java) ?: return null
      return PsiTreeUtil.findChildOfType(tag, Jinja2FunctionCall::class.java)
    }

    fun getReferencedName(refCall: Jinja2FunctionCall): String? {
      return PsiTreeUtil.findChildOfType(refCall, Jinja2StringLiteral::class.java)?.value
    }

    fun findLastSelectQuery(psiFile: PsiFile): SqlQueryExpression? {
      var result: SqlQueryExpression? = null
      val visitor = object: PsiElementVisitor() {
        override fun visitElement(element: PsiElement) {
          super.visitElement(element)

          if (element is SqlQueryExpression) {
            result = element
          }

          for (child in element.children) {
            visitElement(child)
          }
        }
      }
      psiFile.accept(visitor)
      return result
    }

    fun isSqlDialect(file: PsiFile): Boolean {
      val language: Language
      val viewProvider = file.viewProvider
      if (viewProvider is TemplateLanguageFileViewProvider) {
        language = viewProvider.templateDataLanguage
      } else {
        language = file.language
      }
      return language is SqlLanguageDialect || language is SqlLanguage
    }

    fun isUnderIgnoredDirectories(virtualFile: VirtualFile, module: Module): Boolean {
      val root = module.rootManager.contentRoots.firstOrNull { VfsUtil.isUnder(virtualFile, setOf(it)) } ?: return true
      var currentFile = virtualFile
      @Suppress("SENSELESS_COMPARISON")
      while (currentFile != null && currentFile != root) {
        if (currentFile.name in DIRECTORIES_TO_IGNORE) {
          return true
        }
        currentFile = currentFile.parent
      }
      return false
    }

    fun getDbtProjectSqlDirectories(module: Module): List<VirtualFile> {
      val dbtDirectory = getDbtDirectory(module) ?: return emptyList()
      val projectDirectory = module.project.guessProjectDir()
      val projectFile = dbtDirectory.findChild(DBT_PROJECT_FILE_NAME) ?: return emptyList()
      val pathStrings = getCachedSqlPathStrings(projectFile, module.project)
      val subDbtDirectoryStrings = pathStrings
        .mapNotNull { dbtDirectory.findFileByRelativePath(it)?.takeIf { f -> f.isValid } }
      if (projectDirectory == null || projectDirectory == dbtDirectory) return subDbtDirectoryStrings

      val projectDirectoryStrings = pathStrings.mapNotNull {
        projectDirectory.findFileByRelativePath(it)?.takeIf { f -> f.isValid } }
      return subDbtDirectoryStrings + projectDirectoryStrings
    }

    private fun getCachedSqlPathStrings(projectFile: VirtualFile, project: Project): List<String> {
      val stamp = projectFile.modificationStamp
      projectFile.getUserData(DBT_PATHS_CACHE_KEY)?.let { (cachedStamp, paths) ->
        if (cachedStamp == stamp) return paths
      }

      val rootYamlFile = PsiManager.getInstance(project).findFile(projectFile)?.let { it as? YAMLFile  } ?: return emptyList()
      val rootMapping = rootYamlFile.documents.firstOrNull()?.firstChild as? YAMLMapping ?: return emptyList()

      val paths = DBT_SQL_PATH_KEYS.flatMap { it ->
        val value = rootMapping.getKeyValueByKey(it)?.value
        if (value is YAMLArrayImpl) value.items.mapNotNull { (it.value as? YAMLScalar)?.textValue } else emptyList()
      }

      projectFile.putUserData(DBT_PATHS_CACHE_KEY, Pair(stamp, paths))
      return paths
    }

    private const val SKIP_SETUP_KEY = "--skip-profile-setup"
    private const val PROFILE_KEY = "--profile"
    private const val PROFILES_DIR_KEY = "--profiles-dir"
  }
}
