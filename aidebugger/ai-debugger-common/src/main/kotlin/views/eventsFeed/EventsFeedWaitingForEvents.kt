package com.intellij.aidebugger.common.views.eventsFeed

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.intellij.aidebugger.common.AiDebuggerBundle
import com.intellij.aidebugger.common.views.commonMessageView
import org.jetbrains.jewel.ui.component.Text


@Composable
fun EventsFeedWaitingForEventsView(modifier: Modifier = Modifier) = commonMessageView(modifier) {
    Text(AiDebuggerBundle.message("aitoolkit.debugger.waitingForEvents"))
}

@Composable
fun NoEventsReceivedView(modifier: Modifier = Modifier) = commonMessageView(modifier) {
    Text(AiDebuggerBundle.message("aitoolkit.debugger.noEventsReceived"))
}