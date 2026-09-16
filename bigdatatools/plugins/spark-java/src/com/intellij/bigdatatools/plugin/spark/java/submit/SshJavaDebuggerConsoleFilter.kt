package com.intellij.bigdatatools.plugin.spark.java.submit

import com.intellij.bigdatatools.coreUi.util.MessagesBundle
import com.intellij.bigdatatools.coreUi.util.executeOnPooledThread
import com.intellij.codeInsight.hints.InlayPresentationFactory
import com.intellij.codeInsight.hints.presentation.PresentationFactory
import com.intellij.codeInsight.hints.presentation.PresentationRenderer
import com.intellij.debugger.DebuggerManager
import com.intellij.debugger.engine.DebugProcess
import com.intellij.debugger.engine.DebugProcessListener
import com.intellij.debugger.impl.attach.JavaAttachDebuggerProvider
import com.intellij.debugger.impl.attach.JavaDebuggerConsoleFilterProvider
import com.intellij.execution.filters.Filter
import com.intellij.execution.filters.Filter.ResultItem
import com.intellij.execution.impl.InlayProvider
import com.intellij.execution.process.ProcessHandler
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorCustomElementRenderer
import com.intellij.openapi.util.Disposer
import com.intellij.ssh.config.unified.SshConfig
import com.intellij.util.net.NetUtils
import com.intellij.xdebugger.impl.XDebuggerManagerImpl
import com.jetbrains.bigdatatools.common.connection.tunnel.BdtSshTunnelService
import com.jetbrains.bigdatatools.common.connection.tunnel.model.ConnectionSshTunnelInfo
import com.jetbrains.bigdatatools.common.util.invokeLater
import com.jetbrains.spark.submit.util.SparkMessagesBundle
import kotlin.random.Random
import kotlin.random.nextUInt

class SshJavaDebuggerConsoleFilter(val sshConfig: SshConfig, val processHandler: ProcessHandler) : Filter {
  override fun applyFilter(line: String, entireLength: Int): Filter.Result? {
    val matcher = JavaDebuggerConsoleFilterProvider.getConnectionMatcher(line) ?: return null

    val transport = matcher.group(1)
    if (transport != "dt_socket") return null
    val address = matcher.group(2).toIntOrNull() ?: return null
    val start = entireLength - line.length

    return Filter.Result(listOf(AttachInlayResult(start + matcher.start(), start + matcher.end(), transport, address, sshConfig, processHandler),
                                ResultItem(0, 0, null)))
  }
  companion object {
    fun getListeningSocketPort(line: String): Int? {
      val matcher = JavaDebuggerConsoleFilterProvider.getConnectionMatcher(line) ?: return null
      val transport = matcher.group(1)
      if (transport != "dt_socket") return null
      return matcher.group(2).toIntOrNull()
    }
  }
}

class AttachInlayResult(highlightStartOffset: Int, highlightEndOffset: Int, val myTransport: String, val myAddress: Int, val sshConfig: SshConfig, val processHandler: ProcessHandler) : ResultItem(highlightStartOffset, highlightEndOffset, null), InlayProvider {
  override fun createInlayRenderer(editor: Editor): EditorCustomElementRenderer {
    val factory = PresentationFactory(editor)
    val presentation = factory.referenceOnHover(
      factory.roundWithBackground(factory.smallText(SparkMessagesBundle.message("inlay.attach.debugger.ssh"))),
      InlayPresentationFactory.ClickListener { event, point ->
        val tunnelInfo = ConnectionSshTunnelInfo(sshConfig = sshConfig,
                                                 remoteHost = NetUtils.getLocalHostString(),
                                                 remotePort = myAddress)
        executeOnPooledThread {
          val project = editor.getProject()
          val tunnelHandler = BdtSshTunnelService.createIfRequiredInternal(project, tunnelInfo, "debugger${Random.nextUInt()}", false)
          if (tunnelHandler != null) {
            DebuggerManager.getInstance(project).addDebugProcessListener(processHandler, object : DebugProcessListener {
              override fun processDetached(debugProcess: DebugProcess, closedByUser: Boolean) {
                Disposer.dispose(tunnelHandler)
              }
            })
            invokeLater {
              JavaAttachDebuggerProvider.attach(myTransport, tunnelHandler.localPort.toString(), null, project)
            }
          }
          else {
            Notifications.Bus.notify(
              XDebuggerManagerImpl.getNotificationGroup().createNotification(
                SparkMessagesBundle.message("inlay.attach.debugger.ssh"),
                MessagesBundle.message("connection.error.common.tunnel.general"),
                NotificationType.ERROR
              )
            )
          }
        }
      }
    )
    return PresentationRenderer(presentation)
  }
}