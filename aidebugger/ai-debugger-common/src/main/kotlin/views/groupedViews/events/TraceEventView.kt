package com.intellij.aidebugger.common.views.groupedViews.events

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.unit.dp
import com.intellij.aidebugger.common.AiDebuggerBundle
import com.intellij.aidebugger.common.AiDebuggerCollector
import com.intellij.aidebugger.common.models.entities.EventType
import com.intellij.aidebugger.common.models.entities.Framework
import com.intellij.aidebugger.common.viewModels.TraceEventVM
import com.intellij.aidebugger.common.views.AIToolkitTheme
import com.intellij.aidebugger.common.views.components.PrettyTextWithTools
import com.intellij.aidebugger.common.views.eventsFeed.ChildrenEventsList
import com.intellij.aidebugger.common.views.eventsFeed.ComponentSelector
import com.intellij.aidebugger.common.views.eventsFeed.EventNode
import org.jetbrains.jewel.ui.component.Icon
import org.jetbrains.jewel.ui.component.IconButton
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.jewel.ui.icon.PathIconKey

@Composable
fun TraceEventView(viewModel: TraceEventVM, modifier: Modifier = Modifier) {
    var rawMode by remember { mutableStateOf(false) }

    val rawJson = @Composable {
        PrettyTextWithTools(
            modifier = Modifier.fillMaxWidth(),
            text = viewModel.rawJson!!
        )
    }

    val toolBar = @Composable {
        IconButton(
            onClick = {
                rawMode = !rawMode
                if (rawMode) AiDebuggerCollector.reportRawShown()
                else AiDebuggerCollector.reportPrettyShown()
            },
        ) {
            Icon(
                key = if (rawMode) AIToolkitTheme.nodeModeNiceIcon else AIToolkitTheme.nodeModeRawIcon,
                contentDescription = null
            )
        }
    }

    val content = @Composable {
        if (viewModel.finished) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 24.dp),
                verticalArrangement = Arrangement.spacedBy(AIToolkitTheme.typicalSpace2)
            ) {
                viewModel.niceVms.forEach { vm ->
                    ComponentSelector(viewModel = vm, modifier = Modifier.fillMaxWidth())
                }
            }
        } else {
            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 24.dp),
                color = AIToolkitTheme.secondaryTextColor,
                text = AiDebuggerBundle.message("aitoolkit.debugger.session.nodeInProgress"),
            )
        }
    }

    val children = @Composable {
        ChildrenEventsList(viewModel.children)
    }

    val nicerViewAvailable = !viewModel.finished || viewModel.niceVms.isNotEmpty()

    EventNode(
        modifier = modifier,
        eventIcon = viewModel.eventType.asIconKey(),
        title = viewModel.name,
        singleLineTitle = viewModel.framework == Framework.Koog,
        defaultExpanded = viewModel.defaultExpanded,
        content = if (nicerViewAvailable && !rawMode) content else rawJson,
        children = children,
        extra = {
            Row(
                modifier = Modifier
                    .height(22.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AIToolkitTheme.typicalSpace1),
            ) {
                viewModel.widgets?.forEach { ComponentSelector(it) }

                if (!viewModel.finished) {
                    val infiniteTransition = rememberInfiniteTransition()
                    val rotation by infiniteTransition.animateFloat(
                        initialValue = 0f,
                        targetValue = 360f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(durationMillis = 1500, easing = LinearEasing),
                            repeatMode = RepeatMode.Restart
                        )
                    )

                    Icon(
                        modifier = Modifier
                            .padding(all = 3.dp)
                            .rotate(rotation),
                        key = AIToolkitTheme.progressIcon,
                        contentDescription = null,
                    )
                }
            }
        },
        mouseOverToolBar = if (!nicerViewAvailable) null else toolBar
    )
}

@Composable
fun EventType.asIconKey(): PathIconKey = when (this) {
    EventType.LlmCall -> AIToolkitTheme.nodeAiIcon
    EventType.ToolCall -> AIToolkitTheme.nodeToolIcon
    EventType.Init,
    EventType.Group,
    EventType.General -> AIToolkitTheme.infoIcon
    EventType.Exception -> AIToolkitTheme.exceptionIcon
}