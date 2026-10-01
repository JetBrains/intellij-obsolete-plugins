package com.intellij.dataWrangler.jupyterPython.operations.custom

import com.intellij.dataWrangler.jupyterPython.engine.PythonDataWranglerContext
import com.intellij.dataWrangler.operations.CommandFactory
import com.intellij.ide.extensionResources.ExtensionsRootType
import com.intellij.ide.plugins.PluginManagerCore
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.Service.Level
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.project.ProjectManagerListener
import com.intellij.openapi.util.registry.Registry
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.newvfs.BulkFileListener
import com.intellij.openapi.vfs.newvfs.events.VFileEvent
import com.intellij.util.messages.impl.subscribeAsFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.jetbrains.annotations.TestOnly
import java.nio.file.Path
import kotlin.time.Duration.Companion.seconds

@Service(Level.APP)
class JupyterCustomCommandFactories(val cs: CoroutineScope) {
  @OptIn(FlowPreview::class)
  val factories: StateFlow<List<CommandFactory<*, PythonDataWranglerContext>>> =
    MutableStateFlow<List<CommandFactory<*, PythonDataWranglerContext>>>(emptyList()).also { ff ->
      cs.launch {
        ff.value = loadCustomFactories()
        folderChangesFlow().debounce(1.seconds).collectLatest {
          ff.value = loadCustomFactories()  //todo: granular update
        }
      }
    }

  private fun folderChangesFlow(): Flow<Unit> =
    ApplicationManager.getApplication().messageBus.subscribeAsFlow<BulkFileListener, Unit>(VirtualFileManager.VFS_CHANGES) {
      object : BulkFileListener {
        override fun after(events: List<VFileEvent>) {
          val root = getRoot() ?: return
          events.forEach {
            val f = it.file
            if (f != null && VfsUtil.isAncestor(root, f, false)) {
              trySend(Unit)
            }
          }
        }
      }
    }

  private suspend fun loadCustomFactories(): List<CommandFactory<*, PythonDataWranglerContext>> {
    val project = awaitAnyProject()
    val customFactories = get(project)
    return customFactories
  }

  @OptIn(InternalCoroutinesApi::class)
  private suspend fun awaitAnyProject(): Project {
    val pm = ProjectManager.getInstance()
    val res = pm.openProjects.firstOrNull()
    if (res != null) return res
    return withContext(Dispatchers.Default) {
      suspendCancellableCoroutine { cont ->
        ApplicationManager.getApplication().messageBus.connect(this).subscribe(
          ProjectManager.TOPIC,
          object : ProjectManagerListener {
            override fun projectOpened(project: Project) {
              cont.resumeWith(Result.success(project))
            }
          }
        )
        pm.openProjects.firstOrNull()?.let { cont.resumeWith(Result.success(it)) }
      }
    }
  }

  fun getById(id: String): CommandFactory<*, PythonDataWranglerContext>? =
    factories.value.find { it.id == id }

  private fun get(project: Project): List<CommandFactory<*, PythonDataWranglerContext>> =
    getRoot()?.run {
      getChildren().flatMap { file ->
        if (file.extension?.equals("py", true) == true) {
          JupyterCustomCommandParser.parseCustomCommands(project, file).map {
            JupyterCustomCommandFactory(it)
          }
        }
        else {
          emptyList()
        }
      }
    } ?: emptyList()


  companion object {

    fun getRoot(): VirtualFile? = if (!isEnabled()) null else
      getRootPath().let {
        LocalFileSystem.getInstance().findFileByNioFile(it)
      }

    fun getRootPath(): Path =
      ExtensionsRootType.getInstance().findResourceDirectory(PluginManagerCore.CORE_ID, "DataWrangler/python", true)

    fun isEnabled(): Boolean =
      Registry.`is`("datawrangler.plugin.custom.commands", false)
    @TestOnly
    fun parseCustomCommandsForTest(project: Project, file: VirtualFile): List<CommandFactory<*, PythonDataWranglerContext>> =
      JupyterCustomCommandParser.parseCustomCommands(project, file).map {
        JupyterCustomCommandFactory(it)
      }
  }
}