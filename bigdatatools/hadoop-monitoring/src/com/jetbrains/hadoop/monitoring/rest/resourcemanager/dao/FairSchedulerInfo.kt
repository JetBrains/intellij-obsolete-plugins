package com.jetbrains.hadoop.monitoring.rest.resourcemanager.dao

class FairSchedulerInfo : SchedulerInfo() {
  var rootQueue: FairSchedulerQueueInfo? = null
}