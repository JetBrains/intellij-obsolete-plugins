package com.intellij.bigdatatools.zeppelin.components.connections.parser

import com.intellij.bigdatatools.zeppelin.dependency.collector.builtin.Repository
import com.intellij.bigdatatools.zeppelin.dependency.collector.builtin.RepositoryAuth
import com.intellij.bigdatatools.zeppelin.dependency.collector.builtin.RepositoryPolicy
import com.intellij.bigdatatools.zeppelin.dependency.collector.builtin.RepositoryProxy
import com.intellij.bigdatatools.zeppelin.dependency.collector.builtin.RepositoryProxyProtocol
import com.intellij.bigdatatools.zeppelin.models.connection.ZeppelinInfo
import com.intellij.bigdatatools.zeppelin.models.notebook.NotebookInfo
import com.intellij.bigdatatools.zeppelin.utils.JsonParser
import com.intellij.credentialStore.OneTimeString

open class ZeppelinVersionAdapter9 : DefaultZeppelinVersionAdapter() {
  override fun isSupport(info: ZeppelinInfo): Boolean = info.versionInt == 9

  override fun parseNotebookInfos(data: Any): List<NotebookInfo> =
    JsonParser.fromValueList(data, NotebookInfo9::class.java).map { it.noteInfo }

  override fun parseRepositories(repos: Any): List<Repository> {
    val repos9 = JsonParser.fromValueList(repos, Repository9::class.java)
    return repos9.map { repo: Repository9 ->
      val proxy = repo.proxy?.let {
        RepositoryProxy(it.type, it.host, it.port, parseAuth9(it.auth))
      }


      Repository(url = repo.url, id = repo.id, proxy = proxy, snapshotPolicy = repo.snapshotPolicy,
                 authentication = parseAuth9(repo.authentication))
    }
  }

  private fun parseAuth9(auth9: Authentications?): RepositoryAuth {
    val authentications = auth9?.authentications
    val username = authentications?.firstOrNull { it.key == "username" }?.value as? String ?: ""
    val passwordList = authentications?.firstOrNull { it.key == "password" }?.value as? List<*>
    val password = if (passwordList != null && passwordList.isNotEmpty())
      "aaaaaaaaaaa"
    else
      ""

    return RepositoryAuth(username, OneTimeString(password))
  }


  data class RepositoryProxy9(val type: RepositoryProxyProtocol = RepositoryProxyProtocol.HTTP,
                              val host: String = "",
                              val port: Int = -1,
                              val auth: Authentications? = null)


  data class Repository9(val id: String, val url: String,
                         val authentication: Authentications? = null,
                         val proxy: RepositoryProxy9? = null,
                         val snapshotPolicy: RepositoryPolicy = RepositoryPolicy())


  data class Authentications(val authentications: List<KeyValue> = emptyList())
  data class KeyValue(val key: String, val value: Any)

  data class NotebookInfo9(val id: String, val path: String) {
    val noteInfo get() = NotebookInfo(id = id, name = path)
  }
}