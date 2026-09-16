package com.intellij.bigdatatools.zeppelin.integration

import com.intellij.bigdatatools.zeppelin.constants.ZeppelinIcons
import com.intellij.bigdatatools.zeppelin.drivers.ZeppelinDriver
import com.intellij.bigdatatools.zeppelin.drivers.ZeppelinFileTypeViewer
import com.intellij.bigdatatools.zeppelin.models.notebook.NotebookInfo
import com.intellij.bigdatatools.zeppelin.rfs.path.ZeppelinRfsPath
import com.intellij.navigation.ChooseByNameContributor
import com.intellij.navigation.ItemPresentation
import com.intellij.navigation.NavigationItem
import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.PsiFileSystemItem
import com.intellij.util.containers.MultiMap
import com.jetbrains.bigdatatools.common.rfs.driver.manager.DriverManager
import com.jetbrains.bigdatatools.common.rfs.util.withSlash
import com.jetbrains.bigdatatools.common.rfs.view.FileTypeViewerManager
import javax.swing.Icon

class ZeppelinNotebookNameContributor : ChooseByNameContributor {
  private val cachedItems = MultiMap<String, Pair<NotebookInfo, ZeppelinDriver>>()

  override fun getNames(project: Project?, includeNonProjectItems: Boolean): Array<String> {
    if (cachedItems.isEmpty) DriverManager.getDrivers(project).filterIsInstance<ZeppelinDriver>().forEach { driver ->
      driver.connectionManager.instanceConnection.cachedNotesInfo?.filter { it.isFile && !it.isInTrash }?.forEach {
        //cachedItems.putValue(it.shortName, Pair(it, driver))
        cachedItems.putValue(it.name, Pair(it, driver))
        cachedItems.putValue(it.id, Pair(it, driver))
      }
    }

    return cachedItems.keySet().toTypedArray()
  }

  override fun getItemsByName(name: String?, pattern: String?, project: Project?, includeNonProjectItems: Boolean): Array<NavigationItem> {
    if (name == null || project == null) return emptyArray()

    val toTypedArray: Array<NavigationItem> = cachedItems[name].map { (info, driver) ->
      NotebookNavigationItem(
        info, driver, project,
        PsiFileFactory.getInstance(project).createFileFromText(info.shortName, ZeppelinLightFileType, "")
      )
    }.toTypedArray()
    return toTypedArray
  }

  private object ZeppelinLightFileType : FileType {
    override fun getName(): String = "Zeppelin Notebook"
    override fun getDescription(): String = ""
    override fun getDefaultExtension(): String = "zppln"
    override fun getIcon(): Icon = ZeppelinIcons.ZEPPELIN_FILE
    override fun isBinary(): Boolean = false
    override fun isReadOnly(): Boolean = false
    override fun getCharset(file: VirtualFile, content: ByteArray): String = "UTF-8"
  }
}

class NotebookNavigationItem(
  private val notebookInfo: NotebookInfo,
  private val driver: ZeppelinDriver,
  private val project: Project,
  private val delegate: PsiFile
) : NavigationItem, PsiFileSystemItem by delegate {
  private val myPresentation = object : ItemPresentation {
    override fun getPresentableText(): String = notebookInfo.shortName
    override fun getLocationString(): String = "[${shortDriverName()}]://${notebookInfo.name}"
    override fun getIcon(unused: Boolean): Icon = ZeppelinIcons.ZEPPELIN_FILE
  }

  fun getFullNoteName(): String {
    val canonicalPath = ZeppelinRfsPath.createRfsPath(notebookInfo.id, notebookInfo.name).canonicalPath
    val urlPart = (driver.connectionManager.config.url.withSlash() + (driver.connectionManager.tunnelUri ?: "")).withSlash()
    return "$urlPart#/notebook$canonicalPath"
  }

  override fun getProject(): Project = project

  override fun getName(): String = "${notebookInfo.name} (${driver.presentableName})"

  override fun getPresentation(): ItemPresentation = myPresentation

  override fun navigate(requestFocus: Boolean) {
    driver.getNotePathById(notebookInfo.id)?.let { rfsPath ->
      driver.getFileStatus(rfsPath).result?.let { fileInfo ->
        FileTypeViewerManager.getInstance(project).openViewer(fileInfo, requestFocus)
      }
    }
  }

  override fun getNavigationElement(): PsiElement = this

  override fun canNavigate(): Boolean = true

  override fun canNavigateToSource(): Boolean = true

  override fun getVirtualFile(): VirtualFile {
    return ZeppelinFileTypeViewer.Util.prepareVirtualFile(project, notebookInfo.shortName, notebookInfo.id, driver.getExternalId())
  }

  private fun shortDriverName(): String {
    val driverName = driver.presentableName
    return if (driverName.length < 26) driverName else driverName.substring(0, 10) + "..." + driverName.takeLast(11)
  }
}