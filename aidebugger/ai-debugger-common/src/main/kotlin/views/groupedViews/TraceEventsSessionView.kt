package com.intellij.aidebugger.common.views.groupedViews

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.intellij.aidebugger.common.AiDebuggerCollector
import com.intellij.aidebugger.common.models.TracesDatasetsRepository
import com.intellij.aidebugger.common.toolWindow.LocalIdeaProject
import com.intellij.aidebugger.common.viewModels.AddToDatasetViewModel
import com.intellij.aidebugger.common.viewModels.EventsGroupVM
import com.intellij.aidebugger.common.viewModels.TraceEventsSessionVM
import com.intellij.aidebugger.common.views.eventsFeed.ComponentSelector
import com.intellij.aidebugger.common.views.eventsFeed.EventsFeedWaitingForEventsView
import com.intellij.aidebugger.common.views.eventsFeed.EventsList
import com.intellij.aidebugger.common.views.eventsFeed.NoEventsReceivedView
import com.intellij.aidebugger.common.views.eventsFeed.ThreadNavigationView
import com.intellij.openapi.components.service
import kotlinx.coroutines.flow.MutableStateFlow
import org.jetbrains.jewel.foundation.lazy.SelectableLazyListState

@Composable
fun TraceEventsSessionView(viewModel: TraceEventsSessionVM, modifier: Modifier = Modifier) {
    val project = LocalIdeaProject.current ?: throw IllegalStateException("Project is not defined")

    val graphMode = remember(viewModel) { MutableStateFlow(false) }
    val eventsListState = remember(viewModel) { SelectableLazyListState(LazyListState()) }
    var snapToTheLastItem by remember(viewModel) { mutableStateOf(true) }

    val requirementsNotMet by viewModel.requirementsNotMet.collectAsState()
    val threads by viewModel.threads.collectAsState()

    val selectedState by viewModel.selectedThread.collectAsState()
    val graphModeState by graphMode.collectAsState()
    val hstate by viewModel.hstate.collectAsState()
    val debugMode by viewModel.debugMode.collectAsState()

    if (requirementsNotMet.isNotMet) {
        RequirementsNotMetView(modifier, customMessage = requirementsNotMet.message)
        return
    }

    if (threads.activeThreads.isEmpty() && threads.finishedThreads.isEmpty()) {
        val isSessionFinished by viewModel.isSessionFinished.collectAsState()

        if (isSessionFinished) {
            NoEventsReceivedView(Modifier.fillMaxSize())
        } else {
            EventsFeedWaitingForEventsView(Modifier.fillMaxSize())
        }
        return
    }

    if (selectedState == null) {
        viewModel.setSelectedThread((threads.activeThreads.firstOrNull() ?: threads.finishedThreads.firstOrNull())?.threadId)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
    ) {
        val datasetsRepo = remember(project) { project.service<TracesDatasetsRepository>() }
        val addToDatasetViewModel = remember(project, viewModel.scope) {
            AddToDatasetViewModel(
                project = project,
                datasetsRepo = datasetsRepo,
                coroutineScope = viewModel.scope
            )
        }

        ThreadNavigationView(
            threads,
            selectedThread = selectedState,
            graphMode = graphModeState,
            debugMode = debugMode,
            onThreadSelected = { newSelectedThread ->
                viewModel.setSelectedThread(newSelectedThread.threadId)
            },
            addToDatasetViewModel = addToDatasetViewModel,
            onGraphModeChanged = { newGraphMode ->
                graphMode.value = newGraphMode

                if (newGraphMode) AiDebuggerCollector.reportGraphShown()
                else AiDebuggerCollector.reportThreadShown()
            },
            onSelectedThreadChanged = { thread, isRunning ->
                addToDatasetViewModel.setSelectedThread(thread, isRunning, hstate)
            },
            onSave = {
                viewModel.saveState(project)
            },
            modifier = Modifier.fillMaxWidth()
        )

        val selectedEvent = selectedState as? EventsGroupVM?

        selectedEvent?.let { selected->
            if (graphModeState) {
                Box(Modifier.fillMaxSize()) {
                    ComponentSelector(selectedEvent.graphVM)
                }
            } else {
                val events by viewModel.events.collectAsState()

                // Update bottom status when user scrolls manually
                LaunchedEffect(
                    key1 = eventsListState.lazyListState.firstVisibleItemIndex,
                    key2 = eventsListState.lazyListState.firstVisibleItemScrollOffset
                ) {
                    val layoutInfo = eventsListState.lazyListState.layoutInfo
                    val lastVisibleItem = layoutInfo.visibleItemsInfo.lastOrNull()

                    snapToTheLastItem = lastVisibleItem != null &&
                                  lastVisibleItem.index == layoutInfo.totalItemsCount - 1 &&
                                  lastVisibleItem.offset + lastVisibleItem.size <= layoutInfo.viewportEndOffset
                }

                // Auto-scroll when events change and user was at bottom
                LaunchedEffect(events) {
                    if (events.isNotEmpty() && snapToTheLastItem) {
                        val listState = eventsListState.lazyListState
                        // Scroll to the end by scrolling to last item with maximum offset
                        listState.animateScrollToItem(
                            index = events.size - 1,
                            scrollOffset = Int.MAX_VALUE
                        )
                    }
                }

                EventsList(
                    eventsListState,
                    events = events
                )
            }
        }
    }
}