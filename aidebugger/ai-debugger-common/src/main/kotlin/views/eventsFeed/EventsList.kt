@file:Suppress("DuplicatedCode")

package com.intellij.aidebugger.common.views.eventsFeed

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.intellij.aidebugger.common.viewModels.EventVM
import com.intellij.aidebugger.common.views.AIToolkitTheme
import com.intellij.aidebugger.common.views.groupedViews.EventsFeedNodeSelector
import org.jetbrains.jewel.foundation.lazy.SelectableLazyColumn
import org.jetbrains.jewel.foundation.lazy.SelectableLazyListState
import org.jetbrains.jewel.foundation.lazy.itemsIndexed
import org.jetbrains.jewel.ui.component.VerticallyScrollableContainer


@Suppress("DEPRECATION")
@Composable
fun EventsList(listState: SelectableLazyListState, events: List<EventVM>, modifier: Modifier = Modifier) {
    VerticallyScrollableContainer(
        scrollState = listState.lazyListState,
        modifier = modifier
            .fillMaxWidth()
            .padding(start = AIToolkitTheme.typicalSpace3, end = AIToolkitTheme.typicalSpace3 * 2)
    ) {
        SelectableLazyColumn(
            state = listState,
        ) {
            itemsIndexed(items = events) { _, item ->
                Column {
                    Spacer(Modifier.height(AIToolkitTheme.typicalSpace4))
                    EventsFeedNodeSelector(
                        viewModel = item,
                    )
                }
            }
        }
    }
}
@Suppress("DEPRECATION")
@Composable
fun ChildrenEventsList(events: List<EventVM>, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
    ) {
        Spacer(Modifier.width(AIToolkitTheme.typicalSpace2))
        Column(
            modifier = Modifier.fillMaxWidth(),
        ) {
            events.forEachIndexed { index, item ->
                val isLast = index == events.lastIndex
                val connectionColor = AIToolkitTheme.nodeConnectionColor
                val connectionLineSize = AIToolkitTheme.connectionLineSize
                val connectionRadius = AIToolkitTheme.typicalSpace2

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .drawBehind { DrawNodeConnection(isLast, connectionColor, connectionLineSize, connectionRadius) }
                ) {
                    Spacer(Modifier.width(connectionRadius))
                    Column {
                        Spacer(Modifier.height(AIToolkitTheme.typicalSpace4))
                        EventsFeedNodeSelector(
                            viewModel = item
                        )
                    }
                }
            }
        }
    }
}

fun DrawScope.DrawNodeConnection(isLast: Boolean, color: Color, connectionLineSize: Dp, connectionRadius: Dp) {
    val offset = 20.dp.toPx()
    val lineEnd = center.copy(x = 0f, y = offset)
    val fullLineEnd = center.copy(x = 0f, y = size.height)
    val arcRadius = connectionRadius.toPx()
    val arcPosition = center.copy(x = 0f, y = offset - arcRadius)

    drawLine(
        color = color,
        strokeWidth = connectionLineSize.toPx(),
        start = center.copy(x = 0f, y = 0f),
        end = if (isLast) lineEnd else fullLineEnd,
    )

    drawArc(
        color = color,
        startAngle = 90f,
        sweepAngle = 90f,
        useCenter = false,
        topLeft = arcPosition,
        size = Size(arcRadius * 2, arcRadius * 2),
        style = Stroke(width = connectionLineSize.toPx())
    )
}



