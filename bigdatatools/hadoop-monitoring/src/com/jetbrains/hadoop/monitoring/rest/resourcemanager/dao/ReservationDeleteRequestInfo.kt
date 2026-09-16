package com.jetbrains.hadoop.monitoring.rest.resourcemanager.dao

/**
 * Simple class represent the request of deleting a given reservation,
 * selected by its id.
 */
class ReservationDeleteRequestInfo {
  var reservationId: String? = null
}