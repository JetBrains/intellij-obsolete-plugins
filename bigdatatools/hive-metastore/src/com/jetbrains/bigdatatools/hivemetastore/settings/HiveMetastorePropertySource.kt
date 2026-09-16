package com.jetbrains.bigdatatools.hivemetastore.settings

import com.intellij.bigdatatools.coreUi.settings.components.RenderableEntity
import com.intellij.bigdatatools.coreUi.util.MessagesBundle
import com.jetbrains.bigdatatools.hivemetastore.utils.HiveMessagesBundle
import org.jetbrains.annotations.Nls

enum class HiveMetastorePropertySource(@Nls override val title: String) : RenderableEntity {
  DIRECT(MessagesBundle.message("settings.property.source.direct")),
  FILE(HiveMessagesBundle.message("settings.property.source.file"));

  override val id = name.lowercase()
}