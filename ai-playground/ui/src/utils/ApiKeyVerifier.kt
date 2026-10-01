package com.intellij.aiplayground.ui.utils

import com.intellij.aiplayground.models.LlmProvider
import com.intellij.aiplayground.models.LlmServiceManager
import com.intellij.aiplayground.models.settings.LlmProviderSettings
import com.intellij.aiplayground.ui.AIPlaygroundUIBundle
import com.intellij.aiplayground.ui.statistics.AIPlaygroundNotificationsIdsHolder.Companion.API_KEY_INVALID
import com.intellij.aiplayground.ui.statistics.AIPlaygroundNotificationsIdsHolder.Companion.API_KEY_VALID
import com.intellij.notification.NotificationAction
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.project.Project
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
object ApiKeyVerifier {
  private val API_KEY_VERIFICATION_NOTIFICATION_GROUP by lazy {
    NotificationGroupManager.getInstance().getNotificationGroup("AIPlayground.Notifications")
  }

  suspend fun verifyApiKey(
    project: Project,
    provider: LlmProvider,
    settings: LlmProviderSettings<*>,
    showSuccessNotification: Boolean = false
  ): Boolean {
    return try {
      withContext(Dispatchers.IO) {
        LlmServiceManager.getInstance(project).testConnection(provider, settings)
      }
      if (showSuccessNotification) {
        showValidApiKeyNotification(project, provider)
      }
      true
    } catch (e: Exception) {
      showInvalidApiKeyNotification(project, provider)
      false
    }
  }

  private fun showInvalidApiKeyNotification(project: Project, provider: LlmProvider) {
    val providerName = provider.displayName
    API_KEY_VERIFICATION_NOTIFICATION_GROUP
      .createNotification(
        AIPlaygroundUIBundle.message("api.key.invalid.title"),
        AIPlaygroundUIBundle.message("api.key.invalid.message", providerName),
        NotificationType.ERROR
      )
      .setDisplayId(API_KEY_INVALID)
      .addAction(NotificationAction.createSimple(AIPlaygroundUIBundle.message("action.open.ai.playground.settings")) {
        ShowSettingsUtil.getInstance().showSettingsDialog(project, AIPlaygroundUIBundle.message("configurable.ai.playground.display.name"))
      })
      .notify(project)
  }
  
  private fun showValidApiKeyNotification(project: Project, provider: LlmProvider) {
    val providerName = provider.displayName
    API_KEY_VERIFICATION_NOTIFICATION_GROUP
      .createNotification(
        AIPlaygroundUIBundle.message("api.key.valid.title"),
        AIPlaygroundUIBundle.message("api.key.valid.message", providerName),
        NotificationType.INFORMATION
      )
      .setDisplayId(API_KEY_VALID)
      .notify(project)
  }
}