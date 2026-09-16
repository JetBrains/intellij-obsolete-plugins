package com.intellij.bigdatatools.zeppelin.settings

import com.intellij.bigdatatools.coreUi.settings.ModificationKey
import com.intellij.bigdatatools.zeppelin.utils.ZepMessagesBundle

object ZeppelinSettingsKeys {
  val ENABLE_ZTOOLS = ModificationKey(ZepMessagesBundle.message("ztools.settings.enable"))
  val ZTOOLS_CONFIG = ModificationKey(ZepMessagesBundle.message("ztools.settings.config"))

  val SPARK_VERSION_KEY = ModificationKey(ZepMessagesBundle.message("settings.version.spark"))
  val HADOOP_VERSION_KEY = ModificationKey(ZepMessagesBundle.message("settings.version.hadoop"))
  val ZEPPELIN_VERSION_KEY = ModificationKey(ZepMessagesBundle.message("settings.zeppelin.version.label"))
  val FLINK_VERSION_KEY = ModificationKey(ZepMessagesBundle.message("settings.version.flink"))
  val SCALA_VERSION_KEY = ModificationKey(ZepMessagesBundle.message("settings.version.scala"))
  val NOTIFICATION_AFTER_KEY = ModificationKey(ZepMessagesBundle.message("settings.notification.after"))
  val NOTIFICATION_ENABLE_KEY = ModificationKey(ZepMessagesBundle.message("settings.notification.enable"))
}