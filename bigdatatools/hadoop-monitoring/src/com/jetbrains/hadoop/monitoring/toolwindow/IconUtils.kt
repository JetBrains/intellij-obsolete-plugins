package com.jetbrains.hadoop.monitoring.toolwindow

import com.intellij.icons.AllIcons
import com.jetbrains.hadoop.monitoring.rest.resourcemanager.dao.NodeState
import com.jetbrains.hadoop.monitoring.rest.resourcemanager.dao.YarnApplicationState
import javax.swing.Icon

object IconUtils {
  fun getIconForNodeStatus(state: NodeState): Icon = when(state) {
    NodeState.NEW -> AllIcons.RunConfigurations.TestNotRan
    NodeState.RUNNING -> AllIcons.RunConfigurations.TestState.Run_run
    NodeState.UNHEALTHY -> AllIcons.RunConfigurations.TestError
    NodeState.DECOMMISSIONED-> AllIcons.RunConfigurations.TestPassed
    NodeState.LOST-> AllIcons.RunConfigurations.TestUnknown
    NodeState.REBOOTED-> AllIcons.RunConfigurations.TestUnknown
    NodeState.DECOMMISSIONING-> AllIcons.RunConfigurations.TestUnknown
    NodeState.SHUTDOWN-> AllIcons.RunConfigurations.TestTerminated
  }

  fun getIconForApplicationStatus(state: YarnApplicationState): Icon = when(state) {
    YarnApplicationState.NEW-> AllIcons.RunConfigurations.TestNotRan
    YarnApplicationState.NEW_SAVING-> AllIcons.RunConfigurations.TestUnknown
    YarnApplicationState.SUBMITTED-> AllIcons.RunConfigurations.TestUnknown
    YarnApplicationState.ACCEPTED-> AllIcons.RunConfigurations.TestUnknown
    YarnApplicationState.RUNNING-> AllIcons.RunConfigurations.TestState.Run_run
    YarnApplicationState.FINISHED-> AllIcons.RunConfigurations.TestPassed
    YarnApplicationState.FAILED-> AllIcons.RunConfigurations.TestFailed
    YarnApplicationState.KILLED-> AllIcons.RunConfigurations.TestTerminated
  }
}