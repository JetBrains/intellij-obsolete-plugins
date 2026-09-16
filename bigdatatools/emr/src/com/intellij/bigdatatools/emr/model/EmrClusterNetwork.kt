package com.intellij.bigdatatools.emr.model

import com.intellij.bigdatatools.coreUi.table.renderers.FieldName
import com.jetbrains.bigdatatools.common.monitoring.data.model.RemoteInfo

data class EmrClusterNetwork(val availabilityZone: String? = null,
                             @field:FieldName("Subnet Id")
                             val subnetId: String? = null,
                             @field:FieldName("Auto Scaling role")
                             val autoScaleRole: String? = null) : RemoteInfo