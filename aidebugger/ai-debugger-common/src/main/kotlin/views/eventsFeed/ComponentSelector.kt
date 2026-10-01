package com.intellij.aidebugger.common.views.eventsFeed

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.intellij.aidebugger.common.extensionPoints.TraceEventViewModelComponentSelector
import com.intellij.aidebugger.common.viewModels.CommonWidgetVM
import com.intellij.aidebugger.common.viewModels.EventVM
import com.intellij.aidebugger.common.viewModels.InputContentViewVM
import com.intellij.aidebugger.common.viewModels.InputStructuredContentViewVM
import com.intellij.aidebugger.common.viewModels.OutputContentViewVM
import com.intellij.aidebugger.common.viewModels.OutputStructuredContentViewVM
import com.intellij.aidebugger.common.viewModels.SimpleGraphVM
import com.intellij.aidebugger.common.viewModels.ToolCallVM
import com.intellij.aidebugger.common.viewModels.ViewModelBase
import com.intellij.aidebugger.common.viewModels.ViewWithToolCallsVM
import com.intellij.aidebugger.common.views.components.dataValueComponentSelector
import com.intellij.aidebugger.common.views.components.isDataValueComponent
import com.intellij.aidebugger.common.views.eventsFeed.subviews.CommonWidgetView
import com.intellij.aidebugger.common.views.eventsFeed.subviews.SubviewInputStructuredViewView
import com.intellij.aidebugger.common.views.eventsFeed.subviews.SubviewInputTextViewView
import com.intellij.aidebugger.common.views.eventsFeed.subviews.SubviewOutputStructuredViewView
import com.intellij.aidebugger.common.views.eventsFeed.subviews.SubviewOutputTextViewView
import com.intellij.aidebugger.common.views.eventsFeed.subviews.SubviewToolCallView
import com.intellij.aidebugger.common.views.eventsFeed.subviews.SubviewViewWithToolCallsView
import com.intellij.aidebugger.common.views.graph.GraphView

@Composable
fun ComponentSelector(viewModel: ViewModelBase, modifier: Modifier = Modifier) {

    when (viewModel) {
        is SimpleGraphVM -> GraphView(viewModel, modifier)

        is InputContentViewVM -> SubviewInputTextViewView(viewModel, modifier)
        is OutputContentViewVM -> SubviewOutputTextViewView(viewModel, modifier)
        is InputStructuredContentViewVM -> SubviewInputStructuredViewView(viewModel, modifier)
        is OutputStructuredContentViewVM -> SubviewOutputStructuredViewView(viewModel, modifier)
        is ViewWithToolCallsVM -> SubviewViewWithToolCallsView(viewModel, modifier)
        is ToolCallVM -> SubviewToolCallView(viewModel, modifier)

        is CommonWidgetVM -> CommonWidgetView(viewModel, modifier)
        else -> {
            if (isDataValueComponent(viewModel)) {
                Column(modifier = modifier) {
                    dataValueComponentSelector(viewModel)
                }
            }

            TraceEventViewModelComponentSelector.EP_NAME.extensionList
                .firstOrNull { it.vmClass.isInstance(viewModel) }
                ?.let { selector ->
                    @Suppress("UNCHECKED_CAST")
                    (selector as? TraceEventViewModelComponentSelector<EventVM>)
                        ?.componentSelector(viewModel as EventVM, modifier)
                }
        }
    }
}

@Composable
fun <T : EventVM> help(viewModel: ViewModelBase, customComponentSelector: TraceEventViewModelComponentSelector<T>) {
    if (customComponentSelector.vmClass.isInstance(viewModel)) {
        @Suppress("UNCHECKED_CAST")
        customComponentSelector.componentSelector(viewModel as T)
    }
}