package com.intellij.aidebugger.evaluation.settings.env

import com.intellij.aidebugger.evaluation.models.llm.LlmProviderType
import com.intellij.aidebugger.evaluation.settings.AIToolkitSettingsConfigurable
import com.intellij.aidebugger.evaluation.settings.AIToolkitSettingsService
import com.intellij.aidebugger.evaluation.settings.AIToolkitUIBundle
import com.intellij.aidebugger.evaluation.settings.models.ProviderInstance
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.components.service
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.dsl.builder.panel
import java.awt.event.HierarchyEvent
import java.awt.event.HierarchyListener
import javax.swing.Action
import javax.swing.JComponent

/**
 * Dialog for selecting which API keys to use from environment variables
 */
class ApiKeysDialog(
    val project: Project,
    private val foundKeys: Map<LlmProviderType, ApiKeysInEnvVariablesService.FoundKey>,
    private val providerNameMap: Map<LlmProviderType, String>
) : DialogWrapper(project) {

    // Map to store the checkbox state for each key
    private val checkboxes = mutableMapOf<ApiKeysInEnvVariablesService.FoundKey, JBCheckBox>()

    init {
        title = AIToolkitUIBundle.message("api.keys.found.title")
        init()
        okAction.putValue(Action.NAME, AIToolkitUIBundle.message("api.keys.import.selected"))
        cancelAction.putValue(Action.NAME, AIToolkitUIBundle.message("api.keys.cancel"))
    }

    override fun createCenterPanel(): JComponent {
        return panel {
            row {
                label(AIToolkitUIBundle.message("api.keys.found.message"))
            }

            for ((key, value) in foundKeys) {
                val providerName = providerNameMap[key] ?: key.displayName

                row {
                    val checkbox = JBCheckBox(
                        AIToolkitUIBundle.message("api.keys.provider.checkbox", providerName, value.envVar),
                        true // Selected by default
                    )
                    checkboxes[value] = checkbox
                    cell(checkbox)
                }
            }
        }
    }


    /**
     * Returns the list of selected keys
     */
    fun getSelectedKeys(): List<ApiKeysInEnvVariablesService.FoundKey> {
        return checkboxes.entries
            .filter { it.value.isSelected }
            .map { it.key }
    }
}

fun showSettingsAndEnvApiKeysDialog(
    project: Project,
    foundKeys: Map<LlmProviderType, ApiKeysInEnvVariablesService.FoundKey>,
): Boolean {
    var returnValue: Boolean? = null

    ShowSettingsUtil.getInstance()
        .showSettingsDialog(project, AIToolkitSettingsConfigurable::class.java) { configurable ->
            val comp: JComponent = configurable.preferredFocusedComponent
            // Wait until the component is actually showing in the Settings dialog
            if (comp.isShowing) showEnvApiKeysDialog(project, foundKeys)
            else comp.addHierarchyListener(object : HierarchyListener {
                override fun hierarchyChanged(e: HierarchyEvent) {
                    if ((e.changeFlags and HierarchyEvent.SHOWING_CHANGED.toLong()) != 0L && comp.isShowing) {
                        comp.removeHierarchyListener(this)
                        ApplicationManager.getApplication().invokeLater(
                            {
                                returnValue = showEnvApiKeysDialog(project, foundKeys)
                                configurable.viewModel.update()
                            },
                            ModalityState.stateForComponent(comp)
                        )
                    }
                }
            })
        }
    return returnValue ?: false
}

fun showEnvApiKeysDialog(
    project: Project,
    foundKeys: Map<LlmProviderType, ApiKeysInEnvVariablesService.FoundKey>,
): Boolean {
    val providerNameMap = LlmProviderType.entries.associateWith { it.displayName }

    val dialog = ApiKeysDialog(project, foundKeys, providerNameMap)
    val ok = dialog.showAndGet()

    if (ok) {
        val selectedKeys = dialog.getSelectedKeys()

        val settingsManager = service<AIToolkitSettingsService>()
        selectedKeys.forEach { foundKey ->
            settingsManager.addInstance(
                ProviderInstance(
                    providerType = foundKey.providerId,
                    name = "${foundKey.providerId.displayName} (Imported)",
                    apiKey = foundKey.apiKey,
                )
            )
        }
        NotificationGroupManager.getInstance()
            .getNotificationGroup("AIAgentsDebugger.Notifications")
            .createNotification(
                AIToolkitUIBundle.message("notification.api.keys.imported"),
                NotificationType.INFORMATION
            )
            .notify(project)

        ApiKeysInEnvVariablesService.getInstance(project).hideNotification()
    }

    return ok
}