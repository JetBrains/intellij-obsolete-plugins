package com.intellij.aidebugger.common.views.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.intellij.aidebugger.common.AiDebuggerBundle
import com.intellij.aidebugger.common.views.AIToolkitTheme
import org.jetbrains.jewel.ui.component.Text

@Composable
fun ShowMore(
    modifier: Modifier = Modifier,
    content: @Composable (isExpanded: Boolean) -> Unit
) {
    val isExpandedState = remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        content(isExpandedState.value)

        defaultMoreBar(isExpandedState)
    }
}

@Composable
fun ShowMoreWithTextBar(
    modifier: Modifier = Modifier,
    content: @Composable (isExpanded: Boolean) -> Unit,
    moreText: (isExpanded: Boolean) -> String
) {
    val theme = AIToolkitTheme.jsonFormatterTheme
    var isExpanded by remember { mutableStateOf(false) }
    val currentMoreText = remember(isExpanded) { moreText(isExpanded) }

    Column(modifier = modifier) {
        content(isExpanded)

        Box(
            modifier = Modifier
                .fillMaxWidth()
        ) {
            Text(
                text = currentMoreText,
                color = theme.keyColor,
                textDecoration = TextDecoration.Underline,
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(4.dp)
                    .clickable { isExpanded = !isExpanded }
            )
        }
    }
}

@Composable
internal fun BoxScope.defaultMoreBar(isExpandedState: MutableState<Boolean>) {
    var isExpanded by isExpandedState
    val theme = AIToolkitTheme.jsonFormatterTheme

    if (!isExpanded) {

        // Shadow gradient that overlaps with the text
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .fillMaxWidth()
                .height(32.dp)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            AIToolkitTheme.panelBackgroundColor.copy(alpha = 0f),
                            AIToolkitTheme.panelBackgroundColor.copy(alpha = 0.8f),
                            AIToolkitTheme.panelBackgroundColor
                        )
                    )
                )
        )

        // "More" button
        Text(
            text = AiDebuggerBundle.message("aitoolkit.feed.node.showMore"),
            color = theme.keyColor,
            textDecoration = TextDecoration.Underline,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(4.dp)
                .clickable { isExpanded = !isExpanded }
        )
    } else {
        Text(
            text = AiDebuggerBundle.message("aitoolkit.feed.node.showLess"),
            color = theme.keyColor,
            textDecoration = TextDecoration.Underline,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(4.dp)
                .clickable { isExpanded = !isExpanded }
        )
    }
}

@Composable
fun opaqueMoreBar(isExpandedState: MutableState<Boolean>) {
    var isExpanded by isExpandedState
    val theme = AIToolkitTheme.jsonFormatterTheme

    Box(
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = AiDebuggerBundle.message("aitoolkit.feed.node.showLess"),
            color = theme.keyColor,
            textDecoration = TextDecoration.Underline,
            modifier = Modifier
                .align(Alignment.Center)
                .padding(4.dp)
                .clickable { isExpanded = !isExpanded }
        )
    }
}