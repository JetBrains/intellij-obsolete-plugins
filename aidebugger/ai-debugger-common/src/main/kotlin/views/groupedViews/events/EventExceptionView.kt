package com.intellij.aidebugger.common.views.groupedViews.events

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.intellij.aidebugger.common.viewModels.ExceptionEventVM
import com.intellij.aidebugger.common.views.AIToolkitTheme
import com.intellij.aidebugger.common.views.components.CollapsableContent
import com.intellij.aidebugger.common.views.eventsFeed.EventNode
import org.jetbrains.jewel.ui.component.Text

@Composable
fun EventExceptionView(viewModel: ExceptionEventVM, modifier: Modifier = Modifier.Companion) {
    EventNode(
        modifier = modifier,
        eventIcon = AIToolkitTheme.exceptionIcon,
        title = "exception",
        content = {
            CollapsableContent(
                header = {
                    Text(
                        color = AIToolkitTheme.primaryTextColor,
                        text = viewModel.exception,
                    )
                },
                content = {
                    Column {
                        viewModel.stackTrace.forEach {
                            Text(
                                color = AIToolkitTheme.secondaryTextColor,
                                text = "${it.filePath}:${it.lineNumber} ${it.functionName}",
                            )
                        }
                    }
                }
            )
        }, extra = {
            Text(
                color = AIToolkitTheme.secondaryTextColor,
                text = "a moment ago"
            )
        })
}