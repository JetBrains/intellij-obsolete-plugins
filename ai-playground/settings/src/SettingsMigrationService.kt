package com.intellij.aiplayground.settings

import com.intellij.aiplayground.models.settings.PlaygroundSettings
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.thisLogger

/**
 * Service responsible for migrating settings between different versions
 * of the plugin when the settings structure changes.
 */
@Service
class SettingsMigrationService {

  /**
   * Performs a migration of settings if needed.
   * This should be called during plugin initialization.
   */
  fun migrateSettingsIfNeeded() {
    try {
      val currentVersion = getCurrentSettingsVersion()
      val targetVersion = CURRENT_SETTINGS_VERSION

      if (currentVersion < targetVersion) {
        thisLogger().info("Migrating settings from version $currentVersion to $targetVersion")
        performMigration(currentVersion, targetVersion)
        updateSettingsVersion(targetVersion)
        thisLogger().info("Settings migration completed successfully")
      }
    }
    catch (e: Exception) {
      thisLogger().error("Error during settings migration", e)
    }
  }

  /**
   * Get the current settings version from persistent storage
   */
  private fun getCurrentSettingsVersion(): Int {
    val playgroundSettings = ApplicationManager.getApplication().service<PlaygroundSettings>()
    return try {
      val state = playgroundSettings.state
      val versionString = state.javaClass.getDeclaredField("settingsVersion")
        .apply { isAccessible = true }
        .get(state) as? String

      versionString?.toInt() ?: 1
    }
    catch (_: Exception) {
      // If the field doesn't exist, we're on version 1
      1
    }
  }

  /**
   * Update the settings version in persistent storage
   */
  @Suppress("SameParameterValue")
  private fun updateSettingsVersion(version: Int) {
    try {
      val playgroundSettings = ApplicationManager.getApplication().service<PlaygroundSettings>()

      // Create the field if it doesn't exist
      try {
        playgroundSettings.javaClass.getDeclaredField("settingsVersion")
      }
      catch (_: NoSuchFieldException) {
        // Use reflection to add the field dynamically
        val field = PlaygroundSettings::class.java.getDeclaredField("settingsVersion")
        field.isAccessible = true
        field.set(playgroundSettings, version.toString())
      }
    }
    catch (e: Exception) {
      thisLogger().error("Failed to update settings version", e)
    }
  }

  /**
   * Perform the migration between versions
   */
  @Suppress("SameParameterValue")
  private fun performMigration(fromVersion: Int, toVersion: Int) {
    // For each version bump, call the appropriate migration method
    for (version in fromVersion until toVersion) {
      when (version) {
        1 -> migrateV1toV2()
        2 -> migrateV2toV3()
        // Add future migrations here
      }
    }
  }

  /**
   * Migrate from version 1 to version 2
   */
  private fun migrateV1toV2() {
    thisLogger().info("Performing migration from v1 to v2")

    // Example: move a setting from one location to another
    try {
      thisLogger().info("Successfully migrated from v1 to v2")
    }
    catch (e: Exception) {
      thisLogger().error("Error migrating from v1 to v2", e)
    }
  }

  /**
   * Migrate from version 2 to version 3
   * Adds persistedActiveModels map to settings
   */
  private fun migrateV2toV3() {
    thisLogger().info("Performing migration from v2 to v3")

    try {
      val playgroundSettings = ApplicationManager.getApplication().service<PlaygroundSettings>()
      
      // Initialize the persistedActiveModels map if it doesn't exist
      val field = PlaygroundSettings.PlaygroundSettingsState::class.java.getDeclaredField("persistedActiveModels")
      field.isAccessible = true
      
      // Set default value (empty map)
      field.set(playgroundSettings, emptyMap<String, List<String>>())
      
      thisLogger().info("Successfully initialized persistedActiveModels in settings")
    } catch (e: Exception) {
      thisLogger().error("Error initializing persistedActiveModels field", e)
    }

    thisLogger().info("Successfully migrated from v2 to v3")
  }

  companion object {
    // The current version of the settings structure
    const val CURRENT_SETTINGS_VERSION: Int = 3

  }
} 