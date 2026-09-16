package com.intellij.bigdatatools.zeppelin.connection

import com.intellij.bigdatatools.coreUi.connection.exception.BdtConnectionException
import com.intellij.bigdatatools.coreUi.connection.exception.BdtUnexpectedConnectionException
import com.intellij.bigdatatools.zeppelin.drivers.ZeppelinDriver
import com.intellij.bigdatatools.zeppelin.models.connection.ZeppelinInfo
import com.intellij.bigdatatools.zeppelin.settings.ZeppelinConnectionData
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.use
import com.jetbrains.bigdatatools.common.rfs.driver.ActivitySource
import com.jetbrains.bigdatatools.common.rfs.driver.ConnectedConnectionStatus
import com.jetbrains.bigdatatools.common.rfs.driver.FailedConnectionStatus

object ZeppelinConnectionTester {
  suspend fun testConnection(project: Project, connectionData: ZeppelinConnectionData): Pair<ZeppelinInfo?, BdtConnectionException?> {
    val driver = connectionData.createDriver(project, isTest = true) as ZeppelinDriver
    return try {
      driver.use {
        when (val status = driver.refreshConnection(ActivitySource.TEST_ACTION)) {
          is ConnectedConnectionStatus -> driver.connectionManager.instanceConnection.zeppelinInfo to null
          is FailedConnectionStatus -> throw status.getException()
        }
      }
    }
    catch (t: Throwable) {
      return null to (if (t is BdtConnectionException) t else BdtUnexpectedConnectionException(t))
    }
  }
}