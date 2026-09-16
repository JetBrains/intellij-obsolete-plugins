package com.jetbrains.hadoop.monitoring.rest.resourcemanager.dao

/**
 * Simple class that represent a reservation.
 */
class ReservationInfo {
  var acceptanceTime: Long = 0
  var user: String? = null
  var resourceAllocations: List<ResourceAllocationInfo> = emptyList()
  var reservationId: String? = null
  var reservationDefinition: ReservationDefinitionInfo? = null
}