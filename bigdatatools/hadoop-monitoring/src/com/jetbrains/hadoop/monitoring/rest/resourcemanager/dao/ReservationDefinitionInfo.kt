package com.jetbrains.hadoop.monitoring.rest.resourcemanager.dao

/**
 * Simple class that represent a reservation definition.
 */
class ReservationDefinitionInfo {
  var arrival: Long = 0
  var deadline: Long = 0
  var reservationRequests: ReservationRequestsInfo? = null
  var reservationName: String? = null
  var priority = 0
  var recurrenceExpression: String? = null
}