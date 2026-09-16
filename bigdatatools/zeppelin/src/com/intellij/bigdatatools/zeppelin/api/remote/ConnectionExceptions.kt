package com.intellij.bigdatatools.zeppelin.api.remote

import com.intellij.bigdatatools.coreUi.connection.exception.BdtAuthenticationException
import com.intellij.bigdatatools.coreUi.connection.exception.BdtGenericConnectionException
import com.intellij.bigdatatools.coreUi.connection.exception.BdtSpecificConnectionException
import com.intellij.bigdatatools.coreUi.connection.exception.impl.RestResponseException
import com.intellij.bigdatatools.coreUi.connection.exception.impl.RestResponseExceptionData
import com.intellij.bigdatatools.zeppelin.utils.ZepMessagesBundle


class RestResponseZeppelinLoginRequiredException(data: RestResponseExceptionData) : RestResponseException(data)

open class ZeppelinNoRightsException(val responseException: RestResponseException) : BdtGenericConnectionException() {
  override val shortDescription: String = "No rights error - ${responseException.actualCode} code"
  override val cause: Throwable? = responseException
}

/**
 * The lack of permissions can occur in other cases than logging in.
 * This exception is for the case where lack of permissions should be interpreted as invalid shiro credentials.
 */
class ZeppelinBadCredentialsException(override val cause: ZeppelinNoRightsException) : ZeppelinNoRightsException(cause.responseException) {
  override val shortDescription: String = "Zeppelin login/password is incorrect"
}

class ZeppelinMissingCredentialsException(override val cause: RestResponseZeppelinLoginRequiredException) : BdtAuthenticationException() {
  override val shortDescription: String = "Zeppelin requires authorization"
}

class ZeppelinClosedConnectionException(val statusCode: Int?, var reason: String?) : BdtAuthenticationException() {
  override val shortDescription: String = ZepMessagesBundle.message("error.connection.closed", statusCode?.toString() ?: "<UNKNOWN>",
                                                                    reason ?: "<UNKNOWN>")
}

class ZeppelinVersionIsNotSupportedException(version: String, url: String) : BdtSpecificConnectionException() {
  override val shortDescription: String = "Zeppelin Version is not supported yet"
  override val message: String = "Unsupported Zeppelin $version version. Url: $url."
}

class ZeppelinTimeoutException(url: String, timeout: Long, override val cause: Throwable?) : BdtGenericConnectionException() {
  override val shortDescription: String = "Timeout connection exception"
  override val message: String = "Cannot connect to Zeppelin on $url during connection timeout ($timeout ms).\n"
}


class ZeppelinWebSocketConnectionException(url: String, override val cause: Throwable) : BdtGenericConnectionException() {
  override val shortDescription: String = "Websocket connection exception"
}

data class ZeppelinBadlyFormedUrlException(private val url: String) : BdtGenericConnectionException() {
  override val shortDescription: String = "URL is badly formed"
  override val message: String = "Zeppelin url: $url is badly formed."
}