package com.jetbrains.hadoop.monitoring.rest.resourcemanager.dao

/**
 * Protocol interface that provides High Availability related primitives to
 * monitor and fail-over the service.
 *
 *
 * This interface could be used by HA frameworks to manage the service.
 */
interface HAServiceProtocol {
  /**
   * An HA service may be in active or standby state. During startup, it is in
   * an unknown INITIALIZING state. During shutdown, it is in the STOPPING state
   * and can no longer return to active/standby states.
   */
  enum class HAServiceState(val value:String) {
    INITIALIZING("initializing"), ACTIVE("active"), STANDBY("standby"), OBSERVER("observer"), STOPPING("stopping");

    override fun toString(): String = value
  }

  enum class RequestSource {
    REQUEST_BY_USER, REQUEST_BY_USER_FORCED, REQUEST_BY_ZKFC
  }

  /**
   * Information describing the source for a request to change state.
   * This is used to differentiate requests from automatic vs CLI
   * failover controllers, and in the future may include epoch
   * information.
   */
  class StateChangeRequestInfo(val source: RequestSource)

  companion object {
    /**
     * Initial version of the protocol
     */
    const val versionID = 1L
  }
}