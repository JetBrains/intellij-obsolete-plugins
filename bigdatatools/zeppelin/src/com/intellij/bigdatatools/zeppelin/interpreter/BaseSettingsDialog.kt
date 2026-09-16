package com.intellij.bigdatatools.zeppelin.interpreter

import com.intellij.bigdatatools.coreUi.connection.exception.BdtConnectionException
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.util.NlsContexts.DialogTitle
import com.jetbrains.bigdatatools.common.util.invokeLater

interface BaseSettingsDialog {

  companion object {
    fun showErrorMessage(@DialogTitle title: String, e: Exception) {
      invokeLater {
        Messages.showErrorDialog(
          (e.cause as? BdtConnectionException)?.shortDescription ?: e.cause?.localizedMessage ?: e.cause?.message
          ?: (e as? BdtConnectionException)?.shortDescription ?: e.localizedMessage ?: e.message,
          title)
      }
    }
  }

  fun apply(): Boolean
  fun isModified(): Boolean
}