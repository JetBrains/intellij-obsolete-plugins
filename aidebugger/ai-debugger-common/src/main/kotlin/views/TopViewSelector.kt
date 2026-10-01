package com.intellij.aidebugger.common.views

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.intellij.aidebugger.common.viewModels.TraceEventsSessionVM
import com.intellij.aidebugger.common.viewModels.ViewModelBase
import com.intellij.aidebugger.common.views.eventsFeed.eventsFeedEmpty
import com.intellij.aidebugger.common.views.groupedViews.TraceEventsSessionView

@Composable
fun TopViewSelector(viewModel: ViewModelBase?, modifier: Modifier = Modifier) = when (viewModel) {
    is TraceEventsSessionVM -> TraceEventsSessionView(viewModel, modifier)
    else -> eventsFeedEmpty(modifier)
}