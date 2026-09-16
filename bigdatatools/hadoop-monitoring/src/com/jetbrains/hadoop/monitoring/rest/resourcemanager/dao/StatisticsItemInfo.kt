package com.jetbrains.hadoop.monitoring.rest.resourcemanager.dao

data class StatisticsItemInfo(var state: YarnApplicationState, var type: String, var count: Long = 0)