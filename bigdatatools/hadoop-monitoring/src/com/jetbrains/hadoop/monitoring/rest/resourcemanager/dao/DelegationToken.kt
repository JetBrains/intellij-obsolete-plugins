package com.jetbrains.hadoop.monitoring.rest.resourcemanager.dao

class DelegationToken {
  var token: String? = null
  var renewer: String? = null
  var owner: String? = null
  var kind: String? = null
  var nextExpirationTime: Long? = null
  var maxValidity: Long? = null
}