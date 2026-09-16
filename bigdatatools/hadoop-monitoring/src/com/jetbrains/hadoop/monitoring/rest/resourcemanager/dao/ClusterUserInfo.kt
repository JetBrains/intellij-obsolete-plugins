package com.jetbrains.hadoop.monitoring.rest.resourcemanager.dao

/**
 * The YARN UI doesn't have centralized login mechanism. While accessing UI2 from kerberized shell, user who is
 * placed the request to YARN need to be displayed in UI. Given requests from UI2 is routed via Proxy, only RM can provide
 * the user who has placed the request. This DAO object help to provide the requested user and also RM logged in user.
 * the response sent by RM is authenticated user instead of proxy user.
 * It is always good to display authenticated user in browser which eliminates lot of confusion to end use.
 */
class ClusterUserInfo {
  // User who has started the RM
  var rmLoginUser: String? = null

  // User who has placed the request
  var requestedUser: String? = null
}