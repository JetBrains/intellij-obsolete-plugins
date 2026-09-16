package com.intellij.bigdatatools.databricks.cli

import com.intellij.bigdatatools.coreUi.rfs.exception.RfsAuthRequiredError
import com.intellij.bigdatatools.databricks.util.DatabricksBundle
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.util.io.NioFiles
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.wm.IdeFocusManager
import com.intellij.platform.ide.progress.withBackgroundProgress
import com.intellij.util.download.DownloadableFileService
import com.intellij.util.io.Decompressor
import com.intellij.util.system.CpuArch
import com.intellij.util.system.LowLevelLocalMachineAccess
import com.intellij.util.system.OS
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.exists

@OptIn(LowLevelLocalMachineAccess::class)
object DatabricksCliManager {
  suspend fun getOrDownloadDatabricksCli(project: Project?, calledByUser: Boolean): Path {
    getOrDownloadDatabricksCliFolderPath(project = project, calledByUser = calledByUser)
    val path = getDatabricksCliPath()
    if (!path.exists())
      error("Databricks CLI does not exist: $path")
    if (!Files.isExecutable(path))
      NioFiles.setExecutable(path)
    return path
  }

  fun isDatabricksInstalled(): Boolean {
    val executePath = getDatabricksCliPath()
    return executePath.exists()
  }

  private suspend fun getOrDownloadDatabricksCliFolderPath(project: Project?, calledByUser: Boolean): Path {
    val executePath = getDatabricksCliPath()
    if (executePath.exists())
      return executePath

    if (!calledByUser) {
      throw RfsAuthRequiredError()
    }
    val curProject = project ?: getLastFocusedOrOpenedProject()

    val withModalProgress = withBackgroundProgress(curProject, DatabricksBundle.message("progress.title.downloading.databricks")) {
      downloadCli()
    }
    return withModalProgress
  }

  private fun getDatabricksCliPath(): Path {
    val filename = OS.CURRENT.getBinaryName("databricks")
    return getDownloadedCliFolder().resolve(filename)
  }

  private fun getDownloadedCliFolder(): Path {
    val homePath = VfsUtil.getUserHomeDir()?.toNioPath() ?: error("Home path is not found")
    return homePath.resolve(".databricks-sdk").resolve(CLI_FOLDER_NAME)
  }

  @Suppress("IO_FILE_USAGE")
  private fun downloadCli(): Path {
    val osType = when (OS.CURRENT) {
      OS.Linux -> "linux"
      OS.macOS -> "darwin"
      OS.Windows -> "windows"
      else -> error("Unsupported OS")
    }

    val architecture = when (CpuArch.CURRENT) {
      CpuArch.ARM64 -> "arm64"
      CpuArch.X86_64 -> "amd64"
      else -> error("Unsupported arch")
    }

    val url =
      "https://github.com/databricks/cli/releases/download/v$SUPPORTED_CLI_VERSION/databricks_cli_${SUPPORTED_CLI_VERSION}_${osType}_$architecture.zip"
    val fileService = DownloadableFileService.getInstance()
    val desc = fileService.createFileDescription(url, CLI_FOLDER_NAME)
    val downloader = fileService.createDownloader(listOf(desc), "Downloading Databricks CLI...")
    val unpackDir = FileUtil.createTempDirectory(SUPPORTED_CLI_VERSION, null)
    val result = downloader.download(unpackDir.parentFile)
    val virtualFile = result.first()?.first ?: error("File is not downloaded")

    val outputDir = getDownloadedCliFolder()
    Decompressor.Zip(virtualFile.toPath()).extract(outputDir)
    return outputDir
  }

  private const val SUPPORTED_CLI_VERSION = "0.218.1"
  private const val CLI_FOLDER_NAME = "databricks_cli_${SUPPORTED_CLI_VERSION}"

  private fun getLastFocusedOrOpenedProject(): Project {
    return IdeFocusManager.getGlobalInstance().lastFocusedFrame?.project ?: ProjectManager.getInstance().openProjects.first()
  }
}
