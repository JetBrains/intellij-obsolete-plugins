package com.intellij.aidebugger.common.views.eventsFeed

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.onClick
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.intellij.aidebugger.common.AiDebuggerBundle
import com.intellij.aidebugger.common.viewModels.SessionThreadVM
import com.intellij.aidebugger.common.viewModels.SessionThreads
import com.intellij.aidebugger.common.views.AIToolkitTheme
import org.jetbrains.annotations.Nls
import org.jetbrains.jewel.foundation.modifier.onHover
import org.jetbrains.jewel.ui.Orientation
import org.jetbrains.jewel.ui.component.Divider
import org.jetbrains.jewel.ui.component.Icon
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.jewel.ui.component.VerticallyScrollableContainer
import org.jetbrains.jewel.ui.icons.AllIconsKeys

@OptIn(ExperimentalFoundationApi::class)
@Suppress("UnstableApiUsage")
@Composable
fun ThreadsSelector(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    SelectableRow(
        hoverColor = AIToolkitTheme.buttonHoverColor,
        selectedColor = AIToolkitTheme.buttonSelectedColor,
        isSelected = isSelected,
        modifier = modifier
            .onClick { onClick() },
    ) {

        Row(
            modifier = Modifier
                .padding(start = 6.dp, end = 4.dp, top = 3.dp, bottom = 3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                modifier = Modifier.weight(1f),
                text = text,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Icon(
                key = AllIconsKeys.General.ChevronDown,
                contentDescription = null
            )
        }
    }
}

@Composable
private fun SelectableRow(
    hoverColor: Color,
    selectedColor: Color,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable (hovered: Boolean) -> Unit
) {
    var isHovered by remember { mutableStateOf(false) }

    Row(
        modifier = modifier
            .background(
                color = if (isSelected) {
                    selectedColor
                } else {
                    if (isHovered) hoverColor else Color.Unspecified
                },
                shape = RoundedCornerShape(6.dp),
            )
            .onHover { isHovered = it }
    ) {
        content(isHovered)
    }
}

@Composable
private fun HoverHighlightRow(
    hoverColor: Color,
    modifier: Modifier = Modifier,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    verticalAlignment: Alignment.Vertical = Alignment.Top,
    content: @Composable (hovered: Boolean) -> Unit
) {
    var isHovered by remember { mutableStateOf(false) }

    Row(
        horizontalArrangement = horizontalArrangement,
        verticalAlignment = verticalAlignment,
        modifier = modifier
            .background(
                color = if (isHovered) hoverColor else Color.Unspecified,
                shape = RoundedCornerShape(6.dp),
            )
            .onHover { isHovered = it }
    ) {
        content(isHovered)
    }
}

@Composable
fun GreenCounter(value: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .background(AIToolkitTheme.greenDotColor, shape = RoundedCornerShape(8.dp))
    ) {
        Text(
            modifier = Modifier.padding(horizontal = 5.dp),
            color = AIToolkitTheme.greenDotTextColor,
            text=value
        )
    }
}

@Composable
fun pxToDp(px: Int): Dp {
    val density = LocalDensity.current
    return with(density) { px.toDp() }
}

@OptIn(ExperimentalFoundationApi::class)
@Suppress("UnstableApiUsage")
@Composable
fun ThreadSelectorDialog(
    threads: SessionThreads,
    modifier: Modifier = Modifier,
    onClick: (SessionThreadVM) -> Unit,
) {
    var dialogWidthPx by remember { mutableStateOf(0) }
    val scrollState = remember { ScrollState(0) }
    val dialogWidthDp = pxToDp(dialogWidthPx)

    Box(
        modifier = modifier
            .background(color = AIToolkitTheme.chipBackground, shape = RoundedCornerShape(6.dp))
            .border(1.dp, AIToolkitTheme.separatorColor, shape = RoundedCornerShape(6.dp))
            .padding(16.dp)
            .widthIn(min = 300.dp, max = 400.dp)  // Prevents stretching
            .heightIn(min = 100.dp, max = 200.dp)
            .onGloballyPositioned { dialogWidthPx = it.size.width }, // Limits vertical expansion
    ) {
        VerticallyScrollableContainer(
            scrollState = scrollState,
            modifier = modifier
                .fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {

                if (threads.activeThreads.count() > 0) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        GroupTitle(AiDebuggerBundle.message("aitoolkit.debugger.session.liveThreads"))

                        Icon(key = AllIconsKeys.Profiler.Rec, contentDescription = null)
                    }

                    threads.activeThreads.forEach {
                        ThreadItem(
                            thread = it,
                            modifier = Modifier
                                .width(dialogWidthDp)
                                .onClick { onClick(it) }
                        )
                    }
                }

                if (threads.finishedThreads.count() > 0) {
                    if (threads.activeThreads.count() > 0) {
                        Divider(
                            thickness = 1.dp,
                            orientation = Orientation.Horizontal,
                            modifier = Modifier
                                .width(dialogWidthDp)
                                .padding(top = 4.dp, bottom = 6.dp),
                            color = AIToolkitTheme.separatorColor
                        )
                    }

                    GroupTitle(AiDebuggerBundle.message("aitoolkit.debugger.session.finishedThreads"))

                    threads.finishedThreads.forEach {
                        ThreadItem(
                            thread = it,
                            modifier = Modifier
                                .width(dialogWidthDp)
                                .onClick { onClick(it) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GroupTitle(@Nls title: String) {
    Text(
        modifier = Modifier.height(24.dp),
        text = title,
        fontSize = 12.sp,
        color = AIToolkitTheme.menuHeaderColor
    )
}

@Composable
private fun ThreadItem(thread: SessionThreadVM, modifier: Modifier = Modifier) {
    HoverHighlightRow(
        modifier = modifier.height(24.dp),
        verticalAlignment = Alignment.CenterVertically,
        hoverColor = AIToolkitTheme.buttonHoverColor
    ) {
        Text(
            modifier = Modifier.padding(horizontal = 5.dp),
            text = thread.title,
            fontSize = 13.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}