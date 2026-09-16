package com.jetbrains.hadoop.monitoring.rest.resourcemanager.dao

import com.jetbrains.hadoop.monitoring.util.HadoopMessagesBundle
import org.jetbrains.annotations.Nls

/** State of a `Node`. */
enum class NodeState(@Nls val text: String) {
  /** New node. */
  NEW(HadoopMessagesBundle.message("node.state.new")),

  /** Running node. */
  RUNNING(HadoopMessagesBundle.message("node.state.running")),

  /**Node is unhealthy. */
  UNHEALTHY(HadoopMessagesBundle.message("node.state.unhealthy")),

  /** Node is out of service. */
  DECOMMISSIONED(HadoopMessagesBundle.message("node.state.decommissioned")),

  /** Node has not sent a heartbeat for some configured time threshold. */
  LOST(HadoopMessagesBundle.message("node.state.lost")),

  /** Node has rebooted. */
  REBOOTED(HadoopMessagesBundle.message("node.state.rebooted")),

  /** Node decommission is in progress. */
  DECOMMISSIONING(HadoopMessagesBundle.message("node.state.decommissioning")),

  /** Node has shutdown gracefully. */
  SHUTDOWN(HadoopMessagesBundle.message("node.state.shutdown"))
}