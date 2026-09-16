package com.jetbrains.hadoop.monitoring.rest.resourcemanager.dao

data class CapacitySchedulerQueueInfoList(var queue: List<CapacitySchedulerLeafQueueInfo> = emptyList())