package com.jetbrains.hadoop.monitoring.rest.resourcemanager.dao

/**
 * Class to encapsulate information about a Resource - the name of the resource,
 * the units(milli, micro, etc), the type(countable), and the value.
 */
class ResourceInformation {
  var name: String? = null
  var units: String? = null
  var resourceType: ResourceTypes? = null
  var value: Long = 0
  var minimumAllocation: Long = 0
  var maximumAllocation: Long = 0

  companion object {
    // Known resource types
    const val MEMORY_URI = "memory-mb"
    const val VCORES_URI = "vcores"
    const val GPU_URI = "yarn.io/gpu"
    const val FPGA_URI = "yarn.io/fpga"
  }
}