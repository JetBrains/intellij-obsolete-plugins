package com.jetbrains.hadoop.monitoring.rest.resourcemanager.dao

/**
 * DAO object represents resource utilization of node and containers.
 */
class ResourceUtilizationInfo {
   var nodePhysicalMemoryMB = 0
   var nodeVirtualMemoryMB = 0
   var nodeCPUUsage = 0.0
   var aggregatedContainersPhysicalMemoryMB = 0
   var aggregatedContainersVirtualMemoryMB = 0
   var containersCPUUsage = 0.0
}