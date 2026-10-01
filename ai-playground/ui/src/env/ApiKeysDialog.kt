package com.intellij.aiplayground.ui.env

import com.intellij.aiplayground.models.ANTHROPIC_PROVIDER
import com.intellij.aiplayground.models.DEEPSEEK_PROVIDER
import com.intellij.aiplayground.models.GEMINI_PROVIDER
import com.intellij.aiplayground.models.LlmProviderId
import com.intellij.aiplayground.models.LlmProviderInstance
import com.intellij.aiplayground.models.LlmServiceManager
import com.intellij.aiplayground.models.MISTRAL_PROVIDER
import com.intellij.aiplayground.models.OPENAI_PROVIDER
import com.intellij.aiplayground.models.settings.ApplicationSettingsManagerService
import com.intellij.aiplayground.models.statistic.PlaygroundCollector
import com.intellij.aiplayground.models.utils.AiPlaygroundCoroutine
import com.intellij.aiplayground.ui.AIPlaygroundUIBundle
import com.intellij.aiplayground.ui.settings.LlmSettingsConfigurable
import com.intellij.aiplayground.ui.statistics.AIPlaygroundNotificationsIdsHolder
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.components.service
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.dsl.builder.panel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.awt.event.HierarchyEvent
import java.awt.event.HierarchyListener
import javax.swing.Action
import javax.swing.JComponent

/**
 * Dialog for selecting which API keys to use from environment variables
 */
class ApiKeysDialog(
  val project: Project,
  private val foundKeys: Map<LlmProviderId, ApiKeysInEnvVariablesService.FoundKey>,
  private val providerNameMap: Map<LlmProviderId, String>
) : DialogWrapper(project) {

  // Map to store the checkbox state for each key
  private val checkboxes = mutableMapOf<ApiKeysInEnvVariablesService.FoundKey, JBCheckBox>()

  init {
    title = AIPlaygroundUIBundle.message("api.keys.found.title")
    init()
    okAction.putValue(Action.NAME, AIPlaygroundUIBundle.message("api.keys.import.selected"))
    cancelAction.putValue(Action.NAME, AIPlaygroundUIBundle.message("api.keys.cancel"))
  }

  override fun createCenterPanel(): JComponent {
    return panel {
      row {
        label(AIPlaygroundUIBundle.message("api.keys.found.message"))
      }

      //val apiKeysInEnvVariables = ApiKeysInEnvVariables.getInstance(project)
      for ((key, value) in foundKeys) {
        val providerName = providerNameMap[key] ?: key.id

        row {
          val checkbox = JBCheckBox(
            AIPlaygroundUIBundle.message("api.keys.provider.checkbox", providerName, value.envVar),
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
  foundKeys: Map<LlmProviderId, ApiKeysInEnvVariablesService.FoundKey>,
): Boolean {
  var returnValue: Boolean? = null

  PlaygroundCollector.logManageProvidersOpened()
  ShowSettingsUtil.getInstance().showSettingsDialog(project, LlmSettingsConfigurable::class.java) { configurable ->
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
  foundKeys: Map<LlmProviderId, ApiKeysInEnvVariablesService.FoundKey>,
): Boolean {
  val providerNameMap = mapOf(
    OPENAI_PROVIDER.id to "OpenAI",
    ANTHROPIC_PROVIDER.id to "Anthropic",
    MISTRAL_PROVIDER.id to "Mistral",
    DEEPSEEK_PROVIDER.id to "Deepseek",
    GEMINI_PROVIDER.id to "Gemini"
  )

  val dialog = ApiKeysDialog(project, foundKeys, providerNameMap)
  val ok = dialog.showAndGet()

  if (ok) {
    val selectedKeys = dialog.getSelectedKeys()

    PlaygroundCollector.logEnvApiKeysAdded(selectedKeys.map { it.providerId })

    val settingsManager = service<ApplicationSettingsManagerService>()
    for (foundKey in selectedKeys) {
      settingsManager.storeProviderApiKey(foundKey.providerId, foundKey.apiKey)

      val provider = when (foundKey.providerId) {
        OPENAI_PROVIDER.id -> OPENAI_PROVIDER
        ANTHROPIC_PROVIDER.id -> ANTHROPIC_PROVIDER
        MISTRAL_PROVIDER.id -> MISTRAL_PROVIDER
        DEEPSEEK_PROVIDER.id -> DEEPSEEK_PROVIDER
        GEMINI_PROVIDER.id -> GEMINI_PROVIDER
        else -> null
      }

      if (provider != null) {
        val llmServiceManager = LlmServiceManager.getInstance(project)
        val settings = llmServiceManager.createProviderSettings(provider)

        val coroutineScope = service<AiPlaygroundCoroutine>().coroutineScope
        coroutineScope.launch {
          val providers = llmServiceManager.configuredProvidersState.first()
          val providerExists = providers.any { it.provider.id == provider.id }

          if (!providerExists) {
            llmServiceManager.addProviderInstance(LlmProviderInstance(
              provider = provider,
              settings = settings,
            ))
          }
        }
      }
    }

    val toolWindow = ToolWindowManager.getInstance(project).getToolWindow("AI Playground")
    toolWindow?.show()

    NotificationGroupManager.getInstance()
      .getNotificationGroup("AIPlayground.Notifications")
      .createNotification(
        AIPlaygroundUIBundle.message("notification.api.keys.imported"),
        NotificationType.INFORMATION
      )
      .setDisplayId(AIPlaygroundNotificationsIdsHolder.API_KEYS_IMPORTED)
      .notify(project)

    ApiKeysInEnvVariablesService.getInstance(project).hideNotification()
  }
  else {
    PlaygroundCollector.logEnvApiKeysDialogCanceled()
  }
  PlaygroundCollector.logEnvApiKeysDialogShown()

  return ok
}