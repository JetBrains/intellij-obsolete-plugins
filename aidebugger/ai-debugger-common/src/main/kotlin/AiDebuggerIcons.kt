package com.intellij.aidebugger.common

import com.intellij.ui.IconManager
import javax.swing.Icon

object AiDebuggerIcons {
    private fun load(path: String): Icon {
        return IconManager.getInstance().getIcon(path, AiDebuggerIcons::class.java.classLoader)
    }

    val TOOLWINDOW_ICON: Icon = load("/icons/toolWindowIcon.svg")
    val REPORT_ISSUE_ICON: Icon = load("/icons/ui/bug.svg")
}