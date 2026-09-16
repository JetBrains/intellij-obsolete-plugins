package com.intellij.bigdatatools.zeppelin.drivers

import com.intellij.bigdatatools.coreUi.util.BdIdeRegistryUtil
import com.intellij.bigdatatools.notebooks.core.api.editor.NotebookEditor
import com.intellij.bigdatatools.notebooks.core.api.executor.NoteExecutable
import com.intellij.bigdatatools.zeppelin.components.instance.RemoteZeppelinInstance
import com.intellij.bigdatatools.zeppelin.components.instance.ZeppelinConnectionManager
import com.intellij.bigdatatools.zeppelin.components.instance.service.ZeppelinFileManager
import com.intellij.bigdatatools.zeppelin.constants.ZeppelinIcons
import com.intellij.bigdatatools.zeppelin.constants.ZeppelinTimeouts
import com.intellij.bigdatatools.zeppelin.dependency.ZeppelinDependencyManager
import com.intellij.bigdatatools.zeppelin.drivers.actions.checkNotebookName
import com.intellij.bigdatatools.zeppelin.drivers.fileinfo.ZeppelinFileInfo
import com.intellij.bigdatatools.zeppelin.drivers.metainfo.ZeppelinMetaInfoProvider
import com.intellij.bigdatatools.zeppelin.drivers.support.NotLoadedEditorDecisionService
import com.intellij.bigdatatools.zeppelin.editor.ZeppelinEditor
import com.intellij.bigdatatools.zeppelin.executor.ZeppelinNoteExecutor
import com.intellij.bigdatatools.zeppelin.file.ZeppelinFileType
import com.intellij.bigdatatools.zeppelin.models.notebook.ZeppelinNotebook
import com.intellij.bigdatatools.zeppelin.rfs.node.ZeppelinRfsDriverTreeNodeBuilder
import com.intellij.bigdatatools.zeppelin.rfs.path.ZeppelinRfsPath
import com.intellij.bigdatatools.zeppelin.rfs.path.zeppelinAbsolutePath
import com.intellij.bigdatatools.zeppelin.settings.ZeppelinConnectionData
import com.intellij.bigdatatools.zeppelin.utils.ZepMessagesBundle
import com.intellij.bigdatatools.zeppelin.ztools.database.introspector.ZtoolsBdtDbIntrospector
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.runBlockingCancellable
import com.intellij.openapi.progress.runBlockingMaybeCancellable
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.Processor
import com.jetbrains.bigdatatools.common.database.BdtDatabaseUtil
import com.jetbrains.bigdatatools.common.database.BdtDbIntrospectable
import com.jetbrains.bigdatatools.common.rfs.driver.DriverBase
import com.jetbrains.bigdatatools.common.rfs.driver.DriverConnectionStatus
import com.jetbrains.bigdatatools.common.rfs.driver.ExportFormat
import com.jetbrains.bigdatatools.common.rfs.driver.FailedConnectionStatus
import com.jetbrains.bigdatatools.common.rfs.driver.FileInfo
import com.jetbrains.bigdatatools.common.rfs.driver.ReadyConnectionStatus
import com.jetbrains.bigdatatools.common.rfs.driver.RfsPath
import com.jetbrains.bigdatatools.common.rfs.search.impl.ListResult
import com.jetbrains.bigdatatools.common.rfs.search.impl.SearchResult
import com.jetbrains.bigdatatools.common.rfs.util.RfsFileUtil
import com.jetbrains.bigdatatools.common.updater.BDTPluginUtil
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeout
import java.io.FileInputStream
import java.io.OutputStream
import java.util.concurrent.Callable
import java.util.concurrent.Future
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class ZeppelinDriver(override val project: Project?,
                     override val connectionData: ZeppelinConnectionData) : DriverBase(),
                                                                            NoteExecutable, BdtDbIntrospectable {
  override val timeout: Duration
    get() = ZeppelinTimeouts.DRIVER_TIMEOUT.milliseconds
  override val isAvailableCopyThroughIoStreams: Boolean = false

  private val notLoadedEditorService = NotLoadedEditorDecisionService(this)
  override val isFileStorage: Boolean = true
  override val isCreateFileSupported: Boolean = false

  val connectionManager: ZeppelinConnectionManager = ZeppelinConnectionManager(project, connectionData)
  private val remoteInstance = RemoteZeppelinInstance(project, connectionManager)

  override val treeNodeBuilder = ZeppelinRfsDriverTreeNodeBuilder()
  override val fileInfoManager: ZeppelinFileManager = ZeppelinFileManager(this)
  val interpreterSettingsManager = remoteInstance.interpreterSettingsManager
  val fileSystem = remoteInstance.fileSystem
  val dependencyManager: ZeppelinDependencyManager? = remoteInstance.dependencyManager

  override val introspector = if (connectionData.isZtoolsEnabled == true && BDTPluginUtil.isDatabaseEnabled())
    ZtoolsBdtDbIntrospector(this).also {
      Disposer.register(this, it)
    }
  else
    null

  init {
    Disposer.register(this, notLoadedEditorService)
    Disposer.register(this, fileInfoManager)
    Disposer.register(this, remoteInstance)
    Disposer.register(this, connectionManager)
  }

  override fun getMetaInfoProvider() = ZeppelinMetaInfoProvider(this)

  override fun doRefreshConnection(calledByUser: Boolean) {
    logger.trace("Call refresh connection. Driver ${System.identityHashCode(this)}, fileSystem: ${System.identityHashCode(fileSystem)}")

    val connectionError = connectionManager.refreshConnectionSync(true)

    if (connectionError != null)
      throw connectionError

    if (connectionData.isZtoolsEnabled == true && BDTPluginUtil.isDatabaseEnabled()) {
      BdtDatabaseUtil.refreshUIForConnection(project, connectionData.innerId)
    }
  }

  override fun doCheckAvailable(): ReadyConnectionStatus {
    return runBlockingCancellable {
      try {
        withTimeout(30.seconds) {
          val result: ReadyConnectionStatus
          while (true) {
            delay(100)
            when (val connectionStatus = isAvailable()) {
              is ReadyConnectionStatus -> {
                result = connectionStatus
                break
              }
              else -> {}
            }
          }
          result
        }
      }
      catch (e: TimeoutCancellationException) {
        FailedConnectionStatus(e)
      }
    }
  }

  override fun doGetFileStatus(path: RfsPath): FileInfo? {
    val rfsPath = fileSystem.getNoteInfoByPath(path as ZeppelinRfsPath) ?: return null
    return ZeppelinFileInfo(this, rfsPath)
  }

  override fun doListStatus(path: RfsPath): List<FileInfo> {
    val notesStatus = fileSystem.listStatus(path)
    val result = notesStatus.map { ZeppelinFileInfo(this, it) }

    if (BdIdeRegistryUtil.isInternalFeaturesAvailable()) {
      logger.info("doListStatus for $path, result is $result")
    }

    return result.sortedWith(compareBy({ !it.isTrashRoot }, { it.isDirectory }, { it.name }))
  }

  override fun createExecutor(noteEditor: NotebookEditor) = ZeppelinNoteExecutor(this, noteEditor)

  override fun ensureOrCreateParentDirectory(fullPath: RfsPath, existingInfo: FileInfo?) {}

  fun exportNote(rfsPath: ZeppelinRfsPath) = fileSystem.export(rfsPath)

  fun importNote(note: ZeppelinNotebook) = fileSystem.importNote(note)

  fun getNotePathById(noteId: String): ZeppelinRfsPath? = fileSystem.getNotePathById(noteId)
  fun createFileInfoByPath(notePath: ZeppelinRfsPath): ZeppelinFileInfo = ZeppelinFileInfo(this, notePath)
  fun restoreAllFromTrash(): Boolean = fileSystem.restoreAll()
  fun emptyTrash(): Boolean = fileSystem.emptyTrash()
  fun addPromiseOpenNote(project: Project, path: ZeppelinRfsPath) = fileSystem.addPromiseOpenFile(project, path)

  override val root: RfsPath = ZeppelinRfsPath.createRoot()
  override val icon = driverIcon
  override val presentableName: String = connectionData.getShowedName()

  override fun doGetHomeInfo(): FileInfo? = doGetFileStatus(ZeppelinRfsPath.createRoot())

  override fun doCreateWriteStream(rfsPath: RfsPath, overwrite: Boolean, create: Boolean): OutputStream {
    throw UnsupportedOperationException()
  }

  fun createNote(path: RfsPath): ZeppelinFileInfo {
    fileSystem.createNote(path.zeppelinAbsolutePath())
    fileInfoManager.waitAppear(path)

    return ZeppelinFileInfo(this, ZeppelinRfsPath.fromRfsPath(path))
  }

  fun isAvailableBlocking(): DriverConnectionStatus = runBlockingMaybeCancellable {
    isAvailable()
  }

  override fun list(rfsPath: RfsPath, batchId: String?, consumer: Processor<ListResult>): Future<*> =
    ApplicationManager.getApplication().executeOnPooledThread(Callable {
      try {
        if (!isAvailableBlocking().isConnected())
          consumer.process(ListResult.empty)
        else {
          consumer.process(ListResult.ofFileInfos(doListStatus(rfsPath)))
        }
      }
      catch (t: Throwable) {
        consumer.process(ListResult.ofError(t))
      }
    })

  override fun searchInConnection(query: String, batchId: String?, consumer: Processor<SearchResult>): Future<*> =
    ApplicationManager.getApplication().executeOnPooledThread(Callable {
      try {
        if (!isAvailableBlocking().isConnected())
          consumer.process(SearchResult.empty)
        else {
          val results = fileSystem.fullTextSearch(query)
          consumer.process(SearchResult(results))
        }
      }
      catch (t: Throwable) {
        consumer.process(SearchResult.ofError(t))
      }
    })

  override fun doIsAvailable() = fileSystem.getConnectionStatus()

  override fun createRfsPath(path: String) = fileSystem.findNoteByPath(path) ?: ZeppelinRfsPath.createFromPath(path)
  override fun allowSameNamedFilesAndDirectories(): Boolean = true
  override fun getExternalId(): String = connectionData.innerId
  override fun validatePath(path: RfsPath): String? = (checkNotebookName(path.zeppelinAbsolutePath()) as? RfsFileUtil.Fail)?.message
  override fun doMkdir(path: RfsPath) {
    //TODO shouldn't we throw UnsupportedOperationException here?
  }

  override fun doGetHomeUri(): String = ""

  override fun importFile(toPath: RfsPath, inputStream: FileInputStream, exportFormat: ExportFormat?) {
    val note = inputStream.reader().use {
      ZeppelinNotebook(it)
    }

    note.performModification {
      note.name = (toPath as ZeppelinRfsPath).canonicalPath
    }

    if (note.cells.isEmpty())
      error(ZepMessagesBundle.message("filesystem.open.corrupted.note.message"))

    importNote(note)
  }

  fun addIsNotLoadedNoteEditor(zeppelinEditor: ZeppelinEditor) = notLoadedEditorService.addEditor(zeppelinEditor)

  override fun isSupportedForUpload(virtualFile: VirtualFile) = virtualFile.extension in setOf("json", ZeppelinFileType.defaultExtension)

  companion object {
    private val logger = Logger.getInstance(this::class.java)

    val driverIcon = ZeppelinIcons.ZEPPELIN
  }
}