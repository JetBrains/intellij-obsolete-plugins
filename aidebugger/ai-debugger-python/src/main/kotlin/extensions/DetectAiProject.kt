package com.intellij.aidebugger.python.extensions

//import com.intellij.openapi.util.Disposer
import com.intellij.aidebugger.common.services.ProjectSettingsService
import com.intellij.aidebugger.common.services.Requirements
import com.intellij.aidebugger.python.utility.hasImportsOfInterest
import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.startup.ProjectActivity
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.newvfs.BulkFileListener
import com.intellij.openapi.vfs.newvfs.events.VFileEvent
import com.intellij.util.messages.MessageBusConnection
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

//class DetectAiProject(
//    private val coroutineScope: CoroutineScope,
//): ProjectActivity {
//    override suspend fun execute(project: Project) {
//        ProjectOfInterestListener(project, coroutineScope)
//    }
//}

@Service(Service.Level.PROJECT)
class AiDebuggerProjectScopeService(private val project: Project) : Disposable {
    private val job = SupervisorJob()
    val scope: CoroutineScope = CoroutineScope(job + Dispatchers.Default)

    override fun dispose() {
        job.cancel()
    }
}

class DetectAiProject : ProjectActivity {
    override suspend fun execute(project: Project) {
        val scope = project.service<AiDebuggerProjectScopeService>().scope
        ProjectOfInterestListener(project, scope)
    }
}

private class ProjectOfInterestListener(
    private val project: Project,
    private val coroutineScope: CoroutineScope
) {
    private val logger = Logger.getInstance(this::class.java)
    private var messageBusConnection: MessageBusConnection? = null

    init {
        coroutineScope.launch {
            if (ProjectSettingsService.getInstance(project).isAiProject) {
                logger.info("The current project is an AI Project — no further checks are needed.")
                return@launch
            }

            val fileEditorManager = FileEditorManager.getInstance(project)

            if (hasImportsOfInterest(
                project = project,
                files = fileEditorManager.openFiles.asSequence(),
                packages = Requirements.importsOfInterest
            )) {
                logger.info("The current project is detected as an AI Project. One of the open files contains the import of interest.")
                onImportsOfInterest()
                return@launch
            }

            messageBusConnection = project.messageBus.connect()

            messageBusConnection?.subscribe(
                topic = FileEditorManagerListener.FILE_EDITOR_MANAGER,
                handler = object : FileEditorManagerListener {
                    override fun fileOpened(source: FileEditorManager, file: VirtualFile) {
                        coroutineScope.launch {
                            if (hasImportsOfInterest(project, file, Requirements.importsOfInterest)) {
                                logger.info("The current project is detected as an AI Project. The user has opened a file that contains the import of interest.")
                                onImportsOfInterest()
                            }
                        }
                    }
                }
            )

            val fileIndex = ProjectFileIndex.getInstance(project)

            messageBusConnection?.subscribe(
                topic = VirtualFileManager.VFS_CHANGES,
                handler = object : BulkFileListener {
                    override fun after(events: MutableList<out VFileEvent>) {
                        for (event in events) {
                            if (event.file == null || !event.file!!.exists()) continue
                            if (!fileIndex.isInContent(event.file!!)) continue

                            coroutineScope.launch {
                                try {
                                    if (hasImportsOfInterest(project, event.file!!, Requirements.importsOfInterest)) {
                                        logger.info("The current project is detected as an AI Project. The user has updated a file that contains the import of interest.")
                                        onImportsOfInterest()
                                    }
                                } catch (e: Exception) {
                                    logger.error(e)
                                }
                            }
                        }
                    }
                }
            )
        }
    }

    private fun onImportsOfInterest() {
        messageBusConnection?.disconnect()
        ProjectSettingsService.getInstance(project).importsOfInterestPresent = true
    }
}