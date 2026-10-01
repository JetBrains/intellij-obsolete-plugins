
package com.intellij.aidebugger.common.views.eventsFeed

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionOnScreen
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.toSize
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.intellij.aidebugger.common.AiDebuggerBundle
import com.intellij.aidebugger.common.AiDebuggerCollector
import com.intellij.aidebugger.common.onboarding.AnchorBus
import com.intellij.aidebugger.common.onboarding.OnboardingAnchorKeys
import com.intellij.aidebugger.common.toolWindow.LocalIdeaProject
import com.intellij.aidebugger.common.viewModels.AddToDatasetViewModel
import com.intellij.aidebugger.common.viewModels.SessionThreadVM
import com.intellij.aidebugger.common.viewModels.SessionThreads
import com.intellij.aidebugger.common.views.AIToolkitTheme
import com.intellij.aidebugger.common.views.modifiers.bottomBorder
import com.intellij.aidebugger.common.views.modifiers.topBorder
import com.intellij.ide.util.PropertiesComponent
import org.jetbrains.jewel.ui.component.Icon
import org.jetbrains.jewel.ui.component.IconButton
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.jewel.ui.component.Tooltip
import org.jetbrains.jewel.ui.icons.AllIconsKeys

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ThreadNavigationView(
    threads: SessionThreads,
    selectedThread: SessionThreadVM?,
    graphMode: Boolean,
    debugMode: Boolean,
    addToDatasetViewModel: AddToDatasetViewModel,
    onThreadSelected: (SessionThreadVM) -> Unit,
    onGraphModeChanged: (Boolean) -> Unit,
    onSelectedThreadChanged: (SessionThreadVM?, Boolean) -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier
) {
    val project = LocalIdeaProject.current ?: throw IllegalStateException("Project is not defined")
    var showThreadPopup by remember { mutableStateOf(false) }
    var addMenuTriggerWidthPx by remember { mutableStateOf(0) }
    var addMenuTriggerHeightPx by remember { mutableStateOf(0) }
    val totalThreads = threads.activeThreads.size + threads.finishedThreads.size

    LaunchedEffect(selectedThread, threads.activeThreads) {
        // TODO: (@gas) use selectedThread later, for now - keep any thread condition
//        val isRunning = selectedThread?.let { thread ->
//            threads.activeThreads.any { it.threadId == thread.threadId }
//        } ?: false
//        onSelectedThreadChanged(selectedThread, isRunning)
        val hasAnyRunning = threads.activeThreads.isNotEmpty()
        onSelectedThreadChanged(selectedThread, hasAnyRunning)
    }

    fun registerOnboardingAnchorAddToDatasetButton(coordinates: LayoutCoordinates) {
        val pos = coordinates.positionOnScreen()
        val size = coordinates.size.toSize()
        if (size.width > 0f && size.height > 0f) {
            AnchorBus.sink?.set(
                OnboardingAnchorKeys.ADD_TO_DATASET_BUTTON,
                Rect(
                    pos.x,
                    pos.y,
                    pos.x + size.width,
                    pos.y + size.height
                )
            )
        }
    }

    Box(
        modifier = modifier
            .topBorder(AIToolkitTheme.toolbarBorderColor)
            .bottomBorder(AIToolkitTheme.toolbarBorderColor)
    ) {
        Row(
            modifier = modifier
                .padding(horizontal = 7.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                modifier = Modifier
                    .padding(5.dp)
                    .weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box {
                    if (showThreadPopup) {
                        Popup(
                            onDismissRequest = { showThreadPopup = false },
                            offset = IntOffset(0, 40),
                            properties = PopupProperties(focusable = true)
                        ) {
                            ThreadSelectorDialog(threads = threads) {
                                onThreadSelected(it)
                                showThreadPopup = false
                            }
                        }
                    }
                }

                if (totalThreads > 1) {
                    ThreadsSelector(
                        text = selectedThread?.title ?: "",
                        isSelected = showThreadPopup,
                        onClick = {
                            showThreadPopup = true
                        }
                    )
                } else {
                    Row(
                        modifier = Modifier
                            .padding(start = 6.dp, end = 4.dp, top = 3.dp, bottom = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            modifier = Modifier.weight(1f),
                            text = selectedThread?.title ?: "",
                            fontSize = 13.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (selectedThread != null) {
                    val addToDatasetIsVisible by addToDatasetViewModel.isVisible.collectAsState()

                    if (debugMode) {
                        IconButton(
                            modifier = Modifier.padding(5.dp),
                            onClick = {
                                onSave()
                            },
                            content = {
                                Row(verticalAlignment = Alignment.Bottom) {
                                    Icon(
                                        key = AllIconsKeys.Actions.Compile,
                                        contentDescription = null,
                                        modifier = Modifier.padding(start = 4.dp, end = 2.dp)
                                    )
                                }
                            },
                        )
                    }

                    if (addToDatasetIsVisible) {
                        Box {
                            AddToDatasetPopup(
                                viewModel = addToDatasetViewModel,
                                triggerWidthPx = addMenuTriggerWidthPx,
                                triggerHeightPx = addMenuTriggerHeightPx
                            )

                            val canAddToDataset by addToDatasetViewModel.canAddToDataset.collectAsState()

                            var isNewFeatureSeen by remember {
                                mutableStateOf(PropertiesComponent.getInstance().getBoolean("ai.debugger.dataset.button.seen", false))
                            }

                            Tooltip(
                                tooltip = { Text("Create a dataset of traces to evaluate your agent’s performance") },
                            ) {
                                IconButton(
                                    modifier = Modifier
                                        .padding(5.dp)
                                        .onGloballyPositioned {
                                            addMenuTriggerWidthPx = it.size.width
                                            addMenuTriggerHeightPx = it.size.height
                                        },
                                    enabled = canAddToDataset,
                                    onClick = {
                                        val framework = addToDatasetViewModel.hierarchicalState.value?.rootEvents?.firstOrNull()?.framework
                                        AiDebuggerCollector.reportAddToDatasetClicked(project, framework)
                                        if (!isNewFeatureSeen) {
                                            isNewFeatureSeen = true
                                            PropertiesComponent.getInstance().setValue("ai.debugger.dataset.button.seen", true)
                                        }
                                        addToDatasetViewModel.togglePopup()
                                        AnchorBus.sink?.setFlag(OnboardingAnchorKeys.THREADS_NAVIGATION_OPEN_START_ONBOARDING, true) // start onboarding
                                    },
                                    content = {
                                        Box {
                                            val labelColor =
                                                if (canAddToDataset) AIToolkitTheme.primaryTextColor
                                                else AIToolkitTheme.disabledTextColor
                                            Row(
                                                verticalAlignment = Alignment.Bottom,
                                                modifier = Modifier
                                                    .onGloballyPositioned {
                                                        registerOnboardingAnchorAddToDatasetButton(it)
                                                    }
                                            ) {
                                                Icon(
                                                    key = AllIconsKeys.General.Add,
                                                    contentDescription = null,
                                                    modifier = Modifier.padding(start = 4.dp, end = 2.dp),
                                                    tint = labelColor
                                                )
                                                Text(
                                                    text = "Add to Dataset",
                                                    modifier = Modifier.padding(start = 2.dp, end = 4.dp),
                                                    color = labelColor
                                                )
                                            }

                                            if (!isNewFeatureSeen) {
                                                Box(
                                                    modifier = Modifier
                                                        .align(Alignment.TopEnd)
                                                        .padding(top = 2.dp, end = 2.dp)
                                                        .size(6.dp)
                                                        .background(Color(0xFF3574F0), CircleShape)
                                                )
                                            }
                                        }
                                    },
                                )
                            }
                        }
                    }

                    IconButton(
                        modifier = Modifier.padding(5.dp),
                        onClick = {
                            onGraphModeChanged(!graphMode)
                        },
                        content = {
                            Row(verticalAlignment = Alignment.Bottom) {
                                Icon(
                                    key = if (graphMode) {
                                        AllIconsKeys.Toolwindows.ToolWindowMessages
                                    } else {
                                        AllIconsKeys.Graph.Layout
                                    },
                                    contentDescription = null,
                                    modifier = Modifier.padding(start = 4.dp, end = 2.dp)
                                )
                                Text(
                                    text = if (!graphMode) {
                                        AiDebuggerBundle.message("aitoolkit.debugger.navigationPanel.graph")
                                    } else {
                                        AiDebuggerBundle.message("aitoolkit.debugger.navigationPanel.events")
                                    },
                                    modifier = Modifier.padding(start = 2.dp, end = 4.dp),
                                )
                            }
                        },
                    )
                }
            }
        }
    }
}