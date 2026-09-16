package com.jetbrains.hadoop.monitoring.rest.resourcemanager.dao

/**
 * Simple class to allow users to send information required to update an
 * existing reservation.
 */
class ReservationUpdateRequestInfo {
  var reservationId: String? = null
  var reservationDefinition: ReservationDefinitionInfo? = null
}