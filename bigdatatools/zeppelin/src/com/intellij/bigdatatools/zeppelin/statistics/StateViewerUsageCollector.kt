package com.intellij.bigdatatools.zeppelin.statistics

import com.intellij.internal.statistic.eventLog.EventLogGroup
import com.intellij.internal.statistic.eventLog.events.EventFields
import com.intellij.internal.statistic.service.fus.collectors.CounterUsagesCollector
import javax.swing.tree.TreePath

internal object StateViewerUsageCollector : CounterUsagesCollector() {
  override fun getGroup() = GROUP

  private val GROUP = EventLogGroup("bdt.stateviewer", 2)

  private val allowedInterpreters = listOf("spark", "pyspark", "errors", "sql", "unknown")
  private val interpreterField = EventFields.String("group", allowedInterpreters)

  // User expanded or collapsed node in Variables TreeView.
  val interactionEvent = GROUP.registerEvent("interacted", interpreterField)

  // User clicked "refresh" button or link.
  val updateEvent = GROUP.registerEvent("variables.refreshed")

  // User opened settings.
  val settingsEvent = GROUP.registerEvent("settings.opened")

  fun getInterpreter(treePath: TreePath?): String {
    var parentPath: TreePath? = treePath
    while (parentPath != null) {
      val found = allowedInterpreters.find { parentPath?.lastPathComponent.toString().endsWith(".$it") }
      if (found != null) {
        return found
      }
      parentPath = parentPath.parentPath
    }

    return "unknown"
  }
}