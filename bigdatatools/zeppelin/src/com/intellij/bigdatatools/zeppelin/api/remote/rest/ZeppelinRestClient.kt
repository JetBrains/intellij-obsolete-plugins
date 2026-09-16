// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.bigdatatools.zeppelin.api.remote.rest

import com.intellij.bigdatatools.coreUi.connection.ConnectionConfig
import com.intellij.bigdatatools.coreUi.connection.ConnectionConst
import com.intellij.bigdatatools.coreUi.connection.ProxySettings
import com.intellij.bigdatatools.coreUi.connection.RedirectExclude
import com.intellij.bigdatatools.coreUi.connection.exception.BdtConnectionException
import com.intellij.bigdatatools.coreUi.connection.exception.BdtUnexpectedRestResponseException
import com.intellij.bigdatatools.coreUi.connection.exception.impl.RestResponseException
import com.intellij.bigdatatools.coreUi.connection.exception.impl.RestResponseExceptionData
import com.intellij.bigdatatools.coreUi.connection.oauth.OauthRedirectExclude
import com.intellij.bigdatatools.coreUi.util.BdtUrlUtils
import com.intellij.bigdatatools.zeppelin.api.remote.RestResponseZeppelinLoginRequiredException
import com.intellij.bigdatatools.zeppelin.api.remote.ZeppelinMissingCredentialsException
import com.intellij.bigdatatools.zeppelin.api.remote.ZeppelinNoRightsException
import com.intellij.bigdatatools.zeppelin.constants.ZeppelinTimeouts
import com.intellij.bigdatatools.zeppelin.settings.ZeppelinConnectionData
import com.intellij.bigdatatools.zeppelin.utils.JsonParser
import com.intellij.credentialStore.Credentials
import com.intellij.openapi.Disposable
import com.intellij.openapi.util.Disposer
import com.jetbrains.bigdatatools.common.connection.BdtRestExceptionHelper
import com.jetbrains.bigdatatools.common.connection.RestClient
import org.apache.http.HttpStatus.SC_FORBIDDEN
import org.apache.http.HttpStatus.SC_UNAUTHORIZED
import org.apache.http.client.CookieStore
import java.net.URI

class ZeppelinRestClient(config: ZeppelinConnectionData, cookieStore: CookieStore, httpBasicAuth: Credentials?, proxySettings: ProxySettings?, val url: String) : Disposable {

  object ZeppelinLoginRedirectExclude : RedirectExclude {
    override fun shouldExclude(redirectUri: String): Boolean {
      return getRedirectZeppelinUri(redirectUri) != null
    }

    private fun getRedirectZeppelinUri(redirectUri: String): URI? {
      val result = BdtUrlUtils.parseUriOrNull(redirectUri)
      return result?.takeIf {
        result.path == "/api/login"
      }
    }

    override fun createException(redirectUri: String, responseInfo: RestResponseExceptionData): RestResponseException {
      return RestResponseZeppelinLoginRequiredException(responseInfo)
    }
  }

  private val apiUrl
    get() = (if (url.startsWith("http")) url else "http://$url") + "/api"

  private val connectionConst = ConnectionConst.getDefault().copy(
    connectionTimeout = ZeppelinTimeouts.CONNECTION_TIMEOUT,
    readTimeout = ZeppelinTimeouts.READ_TIMEOUT
  )
  private val restClient = RestClient(ConnectionConfig(
    url = apiUrl,
    connectionGroupId = config.groupId,
    basicAuthCredentials = httpBasicAuth,
    proxy = proxySettings,
    cookieStore = cookieStore,
    redirectExcludes = listOf(OauthRedirectExclude, ZeppelinLoginRedirectExclude),
    headers = config.headers ?: emptyMap()
  ), connectionConst)

  init {
    Disposer.register(this, restClient)
  }

  override fun dispose() {}

  fun performGetRequest(uri: String, requestDescription: String): RestResponseMessage {
    return performRequest {
      restClient.performGetRequest(uri, emptyMap(), requestDescription)
    }
  }

  fun performPostData(uri: String,
                      data: Map<String, Any?>,
                      requestDescription: String): RestResponseMessage {
    return performRequest {
      restClient.performPostData(uri, data, emptyMap(), requestDescription)
    }
  }

  fun performDeleteData(uri: String, requestDescription: String): RestResponseMessage {
    return performRequest {
      restClient.performDeleteData(uri, emptyMap(), requestDescription)
    }
  }

  fun performPutData(uri: String, data: Map<String, Any?>,
                     requestDescription: String): RestResponseMessage {
    val headers = mapOf("Charset" to "UTF-8", "Content-Type" to "application/json")
    return performRequest {
      restClient.performPutData(uri, data, headers, requestDescription)
    }
  }

  fun performPostForm(uri: String, data: Map<String, String>,
                      requestDescription: String): RestResponseMessage {
    val headers = mapOf("Charset" to "UTF-8", "Content-Type" to "application/x-www-form-urlencoded")
    return restExceptionHelper.wrapRest {
      val response = restClient.performPostForm(uri, data, headers, requestDescription)
      JsonParser.parseStringJsonToObject(response.second, RestResponseMessage::class.java)
    }
  }

  private fun performRequest(request: () -> String): RestResponseMessage {
    return restExceptionHelper.wrapRest {
      val response = request()
      JsonParser.parseStringJsonToObject(response, RestResponseMessage::class.java)
    }
  }

  private val restExceptionHelper = object : BdtRestExceptionHelper() {
    override fun transformToResponseException(e: RestResponseException): BdtConnectionException {
      return super.transformToResponseException(e).let { result ->
        when {
          result !is BdtUnexpectedRestResponseException -> {
            result
          }
          e.actualCode in listOf(SC_FORBIDDEN, SC_UNAUTHORIZED) -> {
            ZeppelinNoRightsException(e)
          }
          e is RestResponseZeppelinLoginRequiredException -> {
            ZeppelinMissingCredentialsException(e)
          }
          else -> {
            result
          }
        }
      }
    }
  }
}

data class RestResponseMessage(val status: String, val message: String = "", val body: Any = Any())