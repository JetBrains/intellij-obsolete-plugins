package com.intellij.bigdatatools.emr.model

import com.intellij.bigdatatools.coreUi.table.renderers.DateRendering
import com.intellij.bigdatatools.coreUi.table.renderers.FieldName
import com.jetbrains.bigdatatools.common.monitoring.data.model.RemoteInfo
import java.util.Date

data class EmrClusterSummary(val id: String = "",
                             val name: String = "",
                             @field:DateRendering
                             val creationDate: Date? = null,
                             @field:DateRendering
                             val endDate: Date? = null,
                             val autoTermination: Boolean,
                             @field:FieldName("Termination protection")
                             val terminationProtected: Boolean,
                             val tags: String = "",
                             @field:FieldName("Master public DNS")
                             val masterPublicDns: String?,
                             val stateChangeReason: String?) : RemoteInfo