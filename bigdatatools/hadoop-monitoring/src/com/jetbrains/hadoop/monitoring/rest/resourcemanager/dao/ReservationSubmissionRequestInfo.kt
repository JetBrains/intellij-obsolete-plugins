package com.jetbrains.hadoop.monitoring.rest.resourcemanager.dao

/**
 * Simple class to allow users to send information required to create an
 * ReservationSubmissionContext which can then be used to submit a reservation.
 */
class ReservationSubmissionRequestInfo {
  val queue: String? = null
  val reservationDefinition: ReservationDefinitionInfo? = null
  val reservationId: String? = null
}