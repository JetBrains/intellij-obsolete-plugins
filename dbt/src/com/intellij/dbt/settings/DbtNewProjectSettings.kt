package com.intellij.dbt.settings

import com.intellij.dbt.DbtBundle

val NEW_PROFILE_OPTION_NAME = DbtBundle.message("dbt.create.project.with.new.profile")

interface DbtNewProjectSettings {
  fun getDbtProfile(): String?

  fun getDbtSettingsDirectory(): String
}