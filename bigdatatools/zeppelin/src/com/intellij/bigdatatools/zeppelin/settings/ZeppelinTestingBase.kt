package com.intellij.bigdatatools.zeppelin.settings

import com.intellij.bigdatatools.coreUi.connection.exception.BdtOauthAuthenticationException
import com.intellij.bigdatatools.coreUi.util.BdIdeRegistryUtil
import com.intellij.bigdatatools.coreUi.util.MessagesBundle
import com.intellij.bigdatatools.zeppelin.api.remote.ZeppelinBadlyFormedUrlException
import com.intellij.bigdatatools.zeppelin.api.remote.ZeppelinNoRightsException
import com.intellij.bigdatatools.zeppelin.api.remote.ZeppelinVersionIsNotSupportedException
import com.intellij.bigdatatools.zeppelin.connection.ZeppelinConnectionTester
import com.intellij.bigdatatools.zeppelin.utils.ZepMessagesBundle
import com.intellij.ide.BrowserUtil
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.jetbrains.bigdatatools.common.connection.oauth.OAuthResponseTask
import com.jetbrains.bigdatatools.common.connection.oauth.OAuthService
import com.jetbrains.bigdatatools.common.rfs.settings.RfsConnectionTestingBase
import com.jetbrains.bigdatatools.common.settings.connections.ConnectionTestingSession
import com.jetbrains.bigdatatools.common.settings.defaultui.ConnectionError
import com.jetbrains.bigdatatools.common.settings.defaultui.ConnectionStatus
import com.jetbrains.bigdatatools.common.settings.defaultui.ConnectionSuccessful
import com.jetbrains.bigdatatools.common.settings.defaultui.ConnectionWarning
import com.jetbrains.bigdatatools.common.settings.defaultui.SettingsPanelCustomizerEx
import org.jetbrains.ide.BuiltInServerManager

class ZeppelinTestingBase(project: Project, settingsCustomizer: SettingsPanelCustomizerEx<ZeppelinConnectionData>?)
  : RfsConnectionTestingBase<ZeppelinConnectionData>(project, settingsCustomizer) {

  override suspend fun ConnectionTestingSession<ZeppelinConnectionData>.checkConnection(): ConnectionStatus {
    val (info, exception) = ZeppelinConnectionTester.testConnection(project, testConnectionData)

    if (exception != null) {
      val oauthService = OAuthService.instance
      if (oauthService != null && exception is BdtOauthAuthenticationException && BdIdeRegistryUtil.isBrowserAddonOauthEnabled()) {
        val port = BuiltInServerManager.getInstance().port
        val oauthTask = OAuthResponseTask(exception.cause.redirectUrl,
                                          testConnectionData.groupId,
                                          port,
                                          testConnectionData.cookieStore,
                                          testConnectionData.proxySettings) {
          runAgainByExternalEvent()
        }.requestTask
        Disposer.register(dialogDisposable, oauthTask)
        oauthService.addTask(oauthTask)
        BrowserUtil.browse(oauthTask.browseUrl)
        return ConnectionWarning(MessagesBundle.message("oauth.settings.status"))
      }
      else {
        logger.info("Zeppelin test connection error", exception)
        val errorAdditionalText = when (exception) {
          is ZeppelinBadlyFormedUrlException -> ZepMessagesBundle.message("error.bad.formed.url")
          is ZeppelinNoRightsException -> ZepMessagesBundle.message("error.check.password")
          is ZeppelinVersionIsNotSupportedException -> ZepMessagesBundle.message("error.version.is.not.supported")
          else -> ZepMessagesBundle.message("error.parse.api.version")
        }
        return ConnectionError(
          error = exception,
          additionalErrorDescription = errorAdditionalText,
          shortDescription = exception.shortDescription
        )
      }
    }
    else {
      return ConnectionSuccessful(ZepMessagesBundle.message("connected.to.zeppelin", info?.version.toString()))
    }
  }

  companion object {
    val logger = thisLogger()
  }
}