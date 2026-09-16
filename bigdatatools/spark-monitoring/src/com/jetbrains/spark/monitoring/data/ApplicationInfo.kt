package com.jetbrains.spark.monitoring.data

import com.jetbrains.bigdatatools.common.monitoring.data.model.RemoteInfo

data class ApplicationInfo(val id: String, val name: String, val attempts: List<ApplicationAttemptInfo>) : RemoteInfo