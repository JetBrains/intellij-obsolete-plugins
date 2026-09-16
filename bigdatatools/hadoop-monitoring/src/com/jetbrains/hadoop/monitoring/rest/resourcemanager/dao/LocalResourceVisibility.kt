package com.jetbrains.hadoop.monitoring.rest.resourcemanager.dao

/**
 * `LocalResourceVisibility` specifies the *visibility*
 * of a resource localized by the `NodeManager`.
 *
 *
 * The *visibility* can be one of:
 *
 *  * [.PUBLIC] - Shared by all users on the node.
 *  *
 * [.PRIVATE] - Shared among all applications of the
 * *same user* on the node.
 *
 *  *
 * [.APPLICATION] - Shared only among containers of the
 * *same application* on the node.
 *
 *
 *
 * @see LocalResource
 *
 * @see ContainerLaunchContext
 *
 * @see ApplicationSubmissionContext
 *
 * @see ContainerManagementProtocol.startContainers
 */
enum class LocalResourceVisibility {
  /**
   * Shared by all users on the node.
   */
  PUBLIC,

  /**
   * Shared among all applications of the *same user* on the node.
   */
  PRIVATE,

  /**
   * Shared only among containers of the *same application* on the node.
   */
  APPLICATION
}