package com.jetbrains.bigdatatools.hivemetastore.settings

import com.intellij.bigdatatools.coreUi.settings.ModificationKey
import com.jetbrains.bigdatatools.hivemetastore.utils.HiveMessagesBundle

object HiveMetastoreSettingsIds {
  val DATABASE_PATTERN: ModificationKey = ModificationKey(HiveMessagesBundle.message("settings.database.pattern"))
  val TABLE_PATTERN: ModificationKey = ModificationKey(HiveMessagesBundle.message("settings.table.pattern"))
  val PROPERTIES_FILE_KEY: ModificationKey = ModificationKey(HiveMessagesBundle.message("settings.properties.folder"))
  val PROPERTIES_SOURCE_KEY: ModificationKey = ModificationKey(HiveMessagesBundle.message("settings.property.source"))
  val GROUP_NAME = HiveMessagesBundle.message("settings.group.name")
  val PROPERTIES_KEY = ModificationKey(HiveMessagesBundle.message("settings.field.properties"))
}