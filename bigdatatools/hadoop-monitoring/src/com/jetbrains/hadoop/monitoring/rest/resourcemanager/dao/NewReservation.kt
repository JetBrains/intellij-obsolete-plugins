package com.jetbrains.hadoop.monitoring.rest.resourcemanager.dao

/**
 *
 * The response sent by the `ResourceManager` to the client for
 * a request to get a new `ReservationId` for submitting reservations
 * using the REST API.
 *
 *
 * Clients can submit a reservation with the returned `ReservationId`.
 *
 *
 * `RMWebServices#createNewReservation(HttpServletRequest)`
 */
class NewReservation {
  private val reservationId: String? = null
}