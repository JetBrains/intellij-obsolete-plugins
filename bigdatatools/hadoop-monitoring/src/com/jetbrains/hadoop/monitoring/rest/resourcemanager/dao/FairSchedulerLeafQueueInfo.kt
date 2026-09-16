package com.jetbrains.hadoop.monitoring.rest.resourcemanager.dao

class FairSchedulerLeafQueueInfo : FairSchedulerQueueInfo() {
  var numPendingApps = 0
  var numActiveApps = 0
}