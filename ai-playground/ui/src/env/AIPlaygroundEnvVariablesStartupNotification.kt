package com.intellij.aiplayground.ui.env

import com.intellij.aiplayground.models.statistic.PlaygroundCollector
import com.intellij.aiplayground.ui.AIPlaygroundUIBundle
import com.intellij.aiplayground.ui.settings.SmartListModel.Companion.AI_PLAYGROUND_SHOW_IMPORT_KEYS_BANNER_IN_SETTINGS
import com.intellij.aiplayground.ui.statistics.AIPlaygroundNotificationsIdsHolder.Companion.API_KEYS_FOUND
import com.intellij.aiplayground.ui.utils.isChinaRegion
import com.intellij.ide.util.PropertiesComponent
import com.intellij.notification.Notification
import com.intellij.notification.NotificationGroup
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.application.EDT
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.annotations.ApiStatus

@ApiStatus.Internal
class AIPlaygroundEnvVariablesStartupNotification : ProjectActivity, DumbAware {
  private val AI_PLAYGROUND_NOTIFICATION_GROUP: NotificationGroup by lazy {
    NotificationGroupManager.getInstance().getNotificationGroup("AIPlayground.Notifications")
  }

  private var lastNotification: Notification? = null

  override suspend fun execute(project: Project) {
    if (isChinaRegion())
      return
    PropertiesComponent.getInstance(project).setValue(AI_PLAYGROUND_SHOW_IMPORT_KEYS_BANNER_IN_SETTINGS, true)

    val apiKeysInEnvVariablesService = ApiKeysInEnvVariablesService.getInstance(project)

    apiKeysInEnvVariablesService.notificationEvents.collect { event ->
      when (event) {
        is ApiKeysInEnvVariablesService.NotificationEvent.Show -> {
          showEnvApiKeysNotification(project)
        }
        is ApiKeysInEnvVariablesService.NotificationEvent.Hide -> {
          lastNotification?.expire()
          lastNotification = null
        }
        is ApiKeysInEnvVariablesService.NotificationEvent.ProcessingComplete -> {
          // Ignore - only for tests
        }
      }
    }
  }

  private suspend fun showEnvApiKeysNotification(project: Project) {
    val apiKeysService = ApiKeysInEnvVariablesService.getInstance(project)
    withContext(Dispatchers.EDT) {
      PlaygroundCollector.logEnvApiKeysStartupNotificationShown()
      val notification = AI_PLAYGROUND_NOTIFICATION_GROUP
        .createNotification(
          content = AIPlaygroundUIBundle.message("api.keys.found.message.notification"),
          type = NotificationType.INFORMATION
        )
        .setDisplayId(API_KEYS_FOUND)

      notification.addAction(DumbAwareAction.create(AIPlaygroundUIBundle.message("api.keys.import")) {
        PlaygroundCollector.logEnvApiKeysStartupNotificationClicked()
        val foundKeys = apiKeysService.newKeys.value
        val ok = showSettingsAndEnvApiKeysDialog(project, foundKeys)

        if (!ok) {
          apiKeysService.cancelStartupNotification()
        }
      })
      notification.addAction(DumbAwareAction.create(AIPlaygroundUIBundle.message("api.keys.cancel")) {
        PlaygroundCollector.logEnvApiKeysStartupNotificationCanceled()
        apiKeysService.cancelStartupNotification()
      })
      notification.addAction(DumbAwareAction.create(AIPlaygroundUIBundle.message("notification.action.do.not.show.again")) {
        apiKeysService.doNotShowAgain()
      })
      notification.notify(project)
      apiKeysService.markNotificationAsShown()
      lastNotification = notification
    }
  }
}
