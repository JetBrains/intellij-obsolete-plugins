package com.intellij.aiplayground.ui.statistics

import com.intellij.notification.impl.NotificationIdsHolder


internal class AIPlaygroundNotificationsIdsHolder : NotificationIdsHolder {
  override fun getNotificationIds(): List<String> = listOf(
    API_KEYS_IMPORTED,
    API_KEYS_FOUND,
    API_KEY_INVALID,
    API_KEY_VALID,
  )

  companion object {
    const val API_KEYS_IMPORTED = "api.keys.imported"
    const val API_KEYS_FOUND = "api.keys.found"
    const val API_KEY_INVALID = "api.key.invalid"
    const val API_KEY_VALID = "api.key.valid"
  }
}
