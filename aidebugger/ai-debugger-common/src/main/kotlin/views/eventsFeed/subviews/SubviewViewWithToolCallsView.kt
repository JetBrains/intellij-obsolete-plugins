package com.intellij.aidebugger.common.views.eventsFeed.subviews

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.intellij.aidebugger.common.AiDebuggerBundle
import com.intellij.aidebugger.common.viewModels.ToolCallVM
import com.intellij.aidebugger.common.viewModels.ViewWithToolCallsVM
import com.intellij.aidebugger.common.views.AIToolkitTheme
import com.intellij.aidebugger.common.views.components.PrettyText
import com.intellij.aidebugger.common.views.modifiers.bottomBorder
import com.intellij.aidebugger.common.views.modifiers.leftBorder
import org.jetbrains.jewel.ui.component.Text

@Composable
fun SubviewViewWithToolCallsView(viewModel: ViewWithToolCallsVM, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(
            text = AiDebuggerBundle.message("aitoolkit.feed.node.toolCalls"),
            fontSize = AIToolkitTheme.regularTextFontSize,
            color = AIToolkitTheme.primaryTextColor,
        )

        Column(modifier = Modifier
            .fillMaxWidth()
            .padding(4.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)

        ) {
            viewModel.toolCalls.forEach {
                SubviewToolCallView(it)
            }
        }
    }
}

@Composable
fun SubviewToolCallView(viewModel: ToolCallVM, modifier: Modifier = Modifier) {

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = AIToolkitTheme.toolsTableBorderColor,
                shape = RoundedCornerShape(4.dp)
            )
    ) {
        Column(
            modifier = modifier.fillMaxWidth()
        ) {
            val toolNameShape = if (viewModel.arguments.isEmpty()) {
                RoundedCornerShape(4.dp)
            } else {
                RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp)
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = AIToolkitTheme.toolsNameBackgroundColor,
                        shape = toolNameShape
                    )
                    .bottomBorder(color = AIToolkitTheme.toolsTableBorderColor)
                    .padding(6.dp)
            ) {
                Text(viewModel.name)
            }

            viewModel.arguments.forEachIndexed { index, argument ->
                val last = index == viewModel.arguments.lastIndex

                Row(modifier = Modifier
                    .fillMaxWidth()
                    .bottomBorder(color = if (!last) AIToolkitTheme.toolsTableBorderColor else Color.Transparent)
                ) {
                    Text(
                        modifier = Modifier
                            .weight(1f)
                            .padding(6.dp),
                        text = argument.name
                    )
                    Column(
                        modifier = Modifier
                            .weight(3f)
                            .leftBorder(color = AIToolkitTheme.toolsTableBorderColor)
                    ) {
                        PrettyText(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(6.dp),
                            text = argument.value,
                            theme = AIToolkitTheme.jsonFormatterTheme.copy(textColor = AIToolkitTheme.tertiaryTextColor)
                        )
                    }
                }
            }
        }
    }
}