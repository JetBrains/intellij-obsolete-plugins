package com.intellij.bigdatatools.zeppelin.dependency.collector.builtin

import com.intellij.credentialStore.OneTimeString

enum class RepositoryProxyProtocol {
  HTTP, HTTPS
}

data class RepositoryPolicy(val enabled: Boolean = true)

data class RepositoryProxy(val type: RepositoryProxyProtocol = RepositoryProxyProtocol.HTTP,
                           val host: String = "",
                           val port: Int = -1,
                           val auth: RepositoryAuth? = RepositoryAuth())

data class Repository(val id: String, val url: String,
                      val authentication: RepositoryAuth? = RepositoryAuth(),
                      val proxy: RepositoryProxy? = null,
                      private val snapshotPolicy: RepositoryPolicy = RepositoryPolicy()) {
  val snapshot
    get() = snapshotPolicy.enabled
}

data class RepositoryAuth(val username: String = "", val password: OneTimeString = OneTimeString("")) {
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is RepositoryAuth) return false

    if (username != other.username) return false
    return password == other.password
  }

  override fun hashCode(): Int {
    var result = username.hashCode()
    result = 31 * result + password.hashCode()
    return result
  }
}