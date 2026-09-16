package com.intellij.bigdatatools.emr.model

import com.intellij.bigdatatools.coreUi.table.renderers.FieldName
import com.jetbrains.bigdatatools.common.monitoring.data.model.RemoteInfo

data class EmrClusterConfiguration(val releaseLabel: String? = null,
                                   val applications: String? = null,
                                   @field:FieldName("Log URI")
                                   val logUri: String? = null,
                                   @field:FieldName("Custom AMI ID")
                                   val customAmiId: String? = null) : RemoteInfo