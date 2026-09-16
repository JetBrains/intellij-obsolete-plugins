package com.jetbrains.bigdatatools.hivemetastore.utils

import com.intellij.bigdatatools.coreUi.ui.components.ConnectionProperty
import org.apache.hadoop.hive.metastore.conf.MetastoreConf

object HiveMetastorePropertiesUtils {
  fun getConnectionProperties() =
    MetastoreConf.ConfVars.entries.map { configKey ->
      ConnectionProperty(
        propertyName = configKey.varname,
        default = configKey.defaultVal?.toString() ?: "",
        meaning = configKey.description,
        rightSideInfo = "")
    }
}