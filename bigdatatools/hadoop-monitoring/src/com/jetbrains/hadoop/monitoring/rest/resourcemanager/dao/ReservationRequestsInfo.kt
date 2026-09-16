package com.jetbrains.hadoop.monitoring.rest.resourcemanager.dao

/**
 * Simple class representing a list of ReservationRequest and the
 * interpreter which capture the semantic of this list (all/any/order).
 */
class ReservationRequestsInfo {
  var reservationRequestsInterpreter = 0
  var reservationRequest: List<ReservationRequestInfo> = emptyList()
}