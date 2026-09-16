package com.intellij.bigdatatools.zeppelin

import com.intellij.bigdatatools.zeppelin.drivers.ZeppelinDriver
import com.intellij.bigdatatools.zeppelin.editor.actions.ZeppelinToggleDistractionFreeModeAction
import com.intellij.bigdatatools.zeppelin.ztools.controller.ZtoolsService
import com.intellij.ide.IdeEventQueue
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.progress.runBlockingCancellable
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.impl.ProjectFrameHelper
import com.jetbrains.bigdatatools.common.rfs.driver.ActivitySource
import com.jetbrains.bigdatatools.common.rfs.driver.manager.DriverManager
import com.intellij.bigdatatools.coreUi.util.executeOnPooledThread
import java.awt.AWTEvent
import java.awt.event.WindowEvent

@Service(Service.Level.PROJECT)
class ZeppelinProjectService(val project: Project) : Disposable {
  init {
    ActionManager.getInstance().replaceAction("ToggleDistractionFreeMode", ZeppelinToggleDistractionFreeModeAction())
    ZtoolsService.initForProject(project)
    onFocusRefreshListener(project, this)
  }

  override fun dispose() {}

  @Suppress("SameParameterValue")
  private fun refreshDisconnectedConnections(project: Project) = executeOnPooledThread {
    runBlockingCancellable {
      val drivers = DriverManager.getDrivers(project)
      drivers.filterIsInstance<ZeppelinDriver>().forEach {
        val isAvailable = it.isAvailable()
        if (isAvailable.isFailed())
          it.refreshConnection(ActivitySource.ZEPPELIN_RECONNECT)
      }
    }
  }

  private fun onFocusRefreshListener(project: Project, disposable: Disposable) {
    var lastRun = System.currentTimeMillis()
    IdeEventQueue.getInstance().addDispatcher(IdeEventQueue.EventDispatcher { e: AWTEvent ->
      if (e !is WindowEvent) return@EventDispatcher false
      if (e.getID() == WindowEvent.WINDOW_LOST_FOCUS) {
        lastRun = System.currentTimeMillis()
        return@EventDispatcher false
      }
      if (e.getID() != WindowEvent.WINDOW_GAINED_FOCUS) return@EventDispatcher false
      val eventProject = ProjectFrameHelper.getFrameHelper(e.window)?.project ?: return@EventDispatcher false
      if (eventProject != project) return@EventDispatcher false
      val oldTime = lastRun
      lastRun = System.currentTimeMillis()
      if (lastRun - oldTime < 10 * 1000) return@EventDispatcher false

      refreshDisconnectedConnections(project)
      return@EventDispatcher false
    }, disposable)
  }

  companion object {
    fun getInstance(project: Project): ZeppelinProjectService = project.getService(ZeppelinProjectService::class.java)
  }
}