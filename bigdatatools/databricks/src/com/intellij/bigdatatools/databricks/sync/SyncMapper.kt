package com.intellij.bigdatatools.databricks.sync

import com.intellij.bigdatatools.databricks.client.DatabricksDataManager
import com.intellij.bigdatatools.databricks.rfs.DatabricksDriver
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.util.NlsSafe
import com.intellij.util.PlatformUtils
import com.jetbrains.bigdatatools.common.rfs.driver.RfsPath
import java.nio.file.Path
import kotlin.io.path.extension
import kotlin.io.path.isDirectory
import kotlin.io.path.nameWithoutExtension
import kotlin.io.path.relativeTo

internal class SyncMapper(val project: Project, val dataManager: DatabricksDataManager) {
  private val baseLocalPath: Path
    get() = project.guessProjectDir()?.toNioPath() ?: Path.of(project.basePath ?: ".")

  // This path should be with linux-style file separators.
  val baseRemotePath: RfsPath
    get() {
      val user = dataManager.getCurrentUser() ?: ""
      val syncPath = dataManager.connectionData.customRemotePath?.replace('\\', '/') ?: "/Users/$user/.ide/${getRemoteSyncName()}"
      return (dataManager.driver as DatabricksDriver).workspaceDriver.createRfsPath(syncPath)
    }

  fun localPathToRemotePath(path: Path): RfsPath {
    val preparedPath = if (path.extension == "ipynb") {
      path.parent.resolve(path.nameWithoutExtension)
    }
    else {
      path
    }

    val relativeLocal = preparedPath.relativeTo(baseLocalPath)
    return baseRemotePath.addRelative(relativeLocal.toString().replace('\\', '/'), relativeLocal.isDirectory())
  }

  @NlsSafe
  fun getRemoteSyncName(): String {
    return "databricks-${PlatformUtils.getPlatformPrefix()}-${project.locationHash}"
  }
}