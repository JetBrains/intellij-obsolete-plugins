package com.intellij.aidebugger.common.views.eventsFeed.subviews

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.intellij.aidebugger.common.AiDebuggerBundle
import com.intellij.aidebugger.common.viewModels.InputContentViewVM
import com.intellij.aidebugger.common.viewModels.InputStructuredContentViewVM
import com.intellij.aidebugger.common.viewModels.OutputContentViewVM
import com.intellij.aidebugger.common.viewModels.OutputStructuredContentViewVM
import com.intellij.aidebugger.common.views.AIToolkitTheme
import com.intellij.aidebugger.common.views.components.PrettyText
import com.intellij.aidebugger.common.views.components.StructuredDataView
import org.jetbrains.jewel.ui.component.Text

@Composable
fun SubviewInputTextViewView(viewModel: InputContentViewVM, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(
            text = AiDebuggerBundle.message("aitoolkit.feed.node.inputContent"),
            fontSize = AIToolkitTheme.regularTextFontSize,
            color = AIToolkitTheme.primaryTextColor,
        )
        PrettyText(
            modifier = modifier.fillMaxWidth(),
            text = viewModel.content,
            theme = AIToolkitTheme.jsonFormatterTheme.copy(textColor = AIToolkitTheme.tertiaryTextColor)
        )
    }
}

@Composable
fun SubviewOutputTextViewView(viewModel: OutputContentViewVM, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(
            text = AiDebuggerBundle.message("aitoolkit.feed.node.outputContent"),
            fontSize = AIToolkitTheme.regularTextFontSize,
            color = AIToolkitTheme.primaryTextColor,
        )
        PrettyText(
            modifier = modifier.fillMaxWidth(),
            text = viewModel.content,
            theme = AIToolkitTheme.jsonFormatterTheme.copy(textColor = AIToolkitTheme.tertiaryTextColor)
        )
    }
}

@Composable
fun SubviewInputStructuredViewView(viewModel: InputStructuredContentViewVM, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(
            text = AiDebuggerBundle.message("aitoolkit.feed.node.inputContent"),
            fontSize = AIToolkitTheme.regularTextFontSize,
            color = AIToolkitTheme.primaryTextColor,
        )
        StructuredDataView(
            modifier = modifier.fillMaxWidth(),
            data = viewModel.content,
        )
    }
}

@Composable
fun SubviewOutputStructuredViewView(viewModel: OutputStructuredContentViewVM, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(
            text = AiDebuggerBundle.message("aitoolkit.feed.node.outputContent"),
            fontSize = AIToolkitTheme.regularTextFontSize,
            color = AIToolkitTheme.primaryTextColor,
        )
        StructuredDataView(
            modifier = modifier.fillMaxWidth(),
            data = viewModel.content,
        )
    }
}