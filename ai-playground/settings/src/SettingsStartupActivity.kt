package com.intellij.aiplayground.settings

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity

/**
 * Activity that runs when the IDE starts up to initialize settings
 * and perform any necessary migrations.
 */
class SettingsStartupActivity : ProjectActivity {
  override suspend fun execute(project: Project) {
    // Initialize settings migration service
    val migrationService = ApplicationManager.getApplication().service<SettingsMigrationService>()

    // Check and perform any needed settings migrations
    migrationService.migrateSettingsIfNeeded()
  }
} 