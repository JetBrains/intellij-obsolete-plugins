package com.jetbrains.hadoop.monitoring.rest.resourcemanager.dao

import java.io.Closeable

/**
 * Service LifeCycle.
 */
interface Service : Closeable {
  /**
   * Service states
   */
  enum class STATE(
    /**
     * An integer value for use in array lookup and JMX interfaces.
     * Although [Enum.ordinal] could do this, explicitly
     * identify the numbers gives more stability guarantees over time.
     */
    val value: Int,
    /**
     * A name of the state that can be used in messages
     */
    private val statename: String) {
    /**
     * Constructed but not initialized
     */
    NOTINITED(0, "NOTINITED"),

    /**
     * Initialized but not started or stopped
     */
    INITED(1, "INITED"),

    /**
     * started and not stopped
     */
    STARTED(2, "STARTED"),

    /**
     * stopped. No further state transitions are permitted
     */
    STOPPED(3, "STOPPED");

    /**
     * Get the integer value of a state
     *
     * @return the numeric value of the state
     */

    /**
     * Get the name of a state
     *
     * @return the state's name
     */
    override fun toString(): String = statename
  }
}