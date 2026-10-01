package com.intellij.aidebugger.evaluation.services

import com.intellij.aidebugger.evaluation.EvaluationBundle
import com.intellij.ide.plugins.PluginManagerCore
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.components.Service
import com.intellij.openapi.extensions.PluginId
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.project.ProjectManagerListener
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.updateSettings.impl.pluginsAdvertisement.installAndEnable
import com.intellij.openapi.util.NlsContexts
import com.intellij.util.text.VersionComparatorUtil
import java.util.logging.Logger

@Service(Service.Level.PROJECT)
class PluginDownloaderService {
    companion object {
        val LOG: Logger = Logger.getLogger(this::class.java.name)
        private val pluginId = PluginId.getId("org.jetbrains.jettrain")
        private val pluginName = PluginManagerCore.getPlugin(pluginId)?.name ?: "Cadence"
        private const val MIN_REQUIRED_VERSION = "1.7.0" //todo update
    }

    @Volatile
    private var pluginInstallIndicator: ProgressIndicator? = null

    fun isCadencePluginInstalled(minVersion: String = MIN_REQUIRED_VERSION): Boolean {
        val descriptor = PluginManagerCore.getPlugin(pluginId) ?: return false
        val installedVersion = descriptor.version ?: return false
        return VersionComparatorUtil.compare(installedVersion, minVersion) >= 0
    }

    fun installCadencePlugin(project: Project) {
        // Cancel installation if the project is closing
        project.messageBus.connect(project).subscribe(ProjectManager.TOPIC,
            object : ProjectManagerListener {
                override fun projectClosing(closingProject: Project) {
                    if (closingProject == project) {
                        cancelPluginInstallation()
                    }
                }
            }
        )

        object : Task.Backgroundable(project, EvaluationBundle.message("eval.plugin.install.task.title", pluginName), true) {
            override fun run(indicator: ProgressIndicator) {
                pluginInstallIndicator = indicator
                try {
                    if (project.isDisposed || indicator.isCanceled) return
                    indicator.text = EvaluationBundle.message("eval.plugin.install.progress")
                    if (project.isDisposed || indicator.isCanceled) return
                    if (isCadencePluginInstalled()) {
                        LOG.info("Required version of $pluginName is already installed (>= $MIN_REQUIRED_VERSION). Skipping installation.")
                        return
                    }
                    try {
                        if (project.isDisposed || indicator.isCanceled) return
                        installAndEnable(project, setOf(pluginId)) {
                            LOG.info("Plugin $pluginName successfully installed.")
                        }
                    } catch (t: Throwable) {
                        notifyError(project, EvaluationBundle.message("eval.plugin.install.error.message", pluginName))
                        LOG.severe("Error installing plugin $pluginName: ${t.message}")
                    }

                } catch (e: Exception) {
                    notifyError(project, EvaluationBundle.message("eval.plugin.install.error.exception", e.message ?: ""))
                    LOG.severe("Error installing plugin $pluginName : ${e.message}")
                } finally {
                    if (pluginInstallIndicator === indicator) {
                        pluginInstallIndicator = null
                    }
                }
            }
        }.queue()
    }

    fun cancelPluginInstallation() {
        pluginInstallIndicator?.cancel()
        pluginInstallIndicator = null
    }

    private fun notifyError(project: Project?, @NlsContexts.DialogMessage message: String) {
        ApplicationManager.getApplication().invokeLater({
            if (project != null && !project.isDisposed) {
                Messages.showErrorDialog(project, message, EvaluationBundle.message("eval.plugin.install.error.title"))
            }
        }, ModalityState.any())
    }
}
