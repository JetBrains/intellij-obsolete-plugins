package com.intellij.aidebugger.common.views.groupedViews

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.intellij.aidebugger.common.viewModels.ExceptionEventVM
import com.intellij.aidebugger.common.viewModels.TraceEventVM
import com.intellij.aidebugger.common.viewModels.ViewModelBase
import com.intellij.aidebugger.common.views.groupedViews.events.EventExceptionView
import com.intellij.aidebugger.common.views.groupedViews.events.TraceEventView

@Composable
fun EventsFeedNodeSelector(viewModel: ViewModelBase, modifier: Modifier = Modifier) {
    when(viewModel) {
        is ExceptionEventVM -> EventExceptionView(viewModel, modifier)
        is TraceEventVM -> TraceEventView(viewModel)
    }
}