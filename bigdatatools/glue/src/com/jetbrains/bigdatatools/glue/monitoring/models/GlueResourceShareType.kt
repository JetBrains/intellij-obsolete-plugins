package com.jetbrains.bigdatatools.glue.monitoring.models

import com.intellij.openapi.util.NlsSafe
import com.jetbrains.bigdatatools.glue.utils.GlueMessagesBundle
import software.amazon.awssdk.services.glue.model.ResourceShareType

enum class GlueResourceShareType(@NlsSafe val title: String, val awsType: ResourceShareType?) {
  LOCAL(GlueMessagesBundle.message("resource.share.type.local"), awsType = null),
  FOREIGN(GlueMessagesBundle.message("resource.share.type.foreign"), awsType = ResourceShareType.FOREIGN),
  ALL(GlueMessagesBundle.message("resource.share.type.all"), awsType = ResourceShareType.ALL);

  companion object {
    fun fromId(id: String?) = entries.firstOrNull { it.awsType?.name == id } ?: LOCAL
  }
}