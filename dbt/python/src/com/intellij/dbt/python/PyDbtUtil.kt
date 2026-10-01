package com.intellij.dbt.python

import com.intellij.dbt.DbtBundle
import com.intellij.dbt.DbtUtils
import com.intellij.dbt.DbtUtils.Companion.generateDbtInitCommandLine
import com.intellij.dbt.DbtUtils.Companion.getProfileNamesFromDir
import com.intellij.dbt.console.getDbtExecutableName
import com.intellij.dbt.detection.DbtService
import com.intellij.dbt.settings.DbtNewProjectSettings
import com.intellij.dbt.settings.NEW_PROFILE_OPTION_NAME
import com.intellij.ide.projectView.ProjectView
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.findFileOrDirectory
import com.intellij.util.concurrency.annotations.RequiresBackgroundThread
import com.jetbrains.python.packaging.management.PythonPackageManager
import com.jetbrains.python.packaging.management.ui.PythonPackageManagerUI
import com.jetbrains.python.packaging.management.ui.installPackagesBackground
import com.jetbrains.python.sdk.PySdkUtil
import kotlinx.coroutines.launch
import java.io.File
import java.io.IOException
import java.nio.file.Paths

private val LOG: Logger = Logger.getInstance(PyDbtUtil::class.java)

private const val WINDOWS_COLORING_PREFIX = "\u001B[0m"

class PyDbtUtil {
  companion object {
    internal val defaultSettingsDirectory: String get() = DbtUtils.getDbtProfilesDir()

    @RequiresBackgroundThread(generateAssertion = false) // Used by old api, can't force thread
    internal fun getAllProfileNames() = buildSet {
      add(NEW_PROFILE_OPTION_NAME)
      addAll(getProfileNamesFromDir(defaultSettingsDirectory))
    }

    suspend fun configureNewDbtProject(
      project: Project,
      baseDir: VirtualFile,
      dbtSettings: DbtNewProjectSettings,
      sdk: Sdk?,
      module: Module,
    ) {
      if (sdk == null || sdk.homePath == null) {
        showError(DbtBundle.message("dbt.create.project.invalid.interpreter"), project)
        return
      }

      val manager = PythonPackageManager.forSdk(project, sdk)
      PythonPackageManagerUI.forPackageManager(manager).installPackagesBackground(listOf("dbt-core")) ?: return

      var tempDir: File? = null
      try {
        tempDir = FileUtil.generateRandomTemporaryPath()
        val created = tempDir.mkdir()
        if (!created) {
          showError(DbtBundle.message("dbt.create.project.failed.to.create.temp.dir"), project)
          return
        }

        val dbtName = getDbtExecutableName()
        var initProfileName = dbtSettings.getDbtProfile()
        if (initProfileName == NEW_PROFILE_OPTION_NAME) {
          initProfileName = null
        }

        val commandLine = generateDbtInitCommandLine(dbtName, baseDir.path, project.name, initProfileName, dbtSettings.getDbtSettingsDirectory())

        @Suppress("DialogTitleCapitalization")
        val output = PySdkUtil.getProcessOutput(commandLine, tempDir.path, PySdkUtil.activateVirtualEnv(sdk), 30000, null, false,
                                                DbtBundle.message("dbt.create.project.title"))

        var dbtInitErrors = output.stderr
        if (dbtInitErrors.startsWith(WINDOWS_COLORING_PREFIX)) {
          dbtInitErrors = dbtInitErrors.substring(WINDOWS_COLORING_PREFIX.length)
        }
        if (dbtInitErrors.isEmpty()) {
          val rootRepoFile = tempDir.listFiles()?.first { file -> file.name == project.name }
          if (rootRepoFile == null) {
            LOG.warn(DbtBundle.message("dbt.create.project.failed.to.get.project.in.temp.dir"))
            return
          }
          for (file in rootRepoFile.listFiles()!!) {
            file.renameTo(Paths.get(baseDir.path, file.name).toFile())
          }
        }
        else {
          showError(output.stderr, project)
        }
        baseDir.refresh(true, true) {
          expandProjectView(baseDir, project)

          val dbtService = project.service<DbtService>()
          dbtService.coroutineScope.launch {
            dbtService.processModule(module)
          }
        }
      }
      catch (e: IOException) {
        showError(DbtBundle.message("dbt.create.project.failed.to.initialize"), project)
      }
      finally {
        if (tempDir != null) {
          if (FileUtil.delete(tempDir)) LOG.info("Temp dir removed: $tempDir")
          else LOG.warn("Failed to remove temp directory: $tempDir")
        }
      }
    }

    private fun expandProjectView(baseDir: VirtualFile, project: Project) {
      val file = baseDir.findFileOrDirectory("models") ?: baseDir.children.first()
      if (file != null) {
        ProjectView.getInstance(project).selectCB(null, file, true)
      }
    }

    private fun showError(errorMessage: String, project: Project) {
      ApplicationManager.getApplication().invokeLater {
        @Suppress("DialogTitleCapitalization")
        (Messages.showErrorDialog(project,
                                  DbtBundle.message("dbt.create.project.error", errorMessage),
                                  DbtBundle.message("dbt.create.project")))
      }
    }
  }
}