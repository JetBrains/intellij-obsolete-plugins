package com.intellij.aidebugger.common.views.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.jetbrains.jewel.ui.component.Icon
import org.jetbrains.jewel.ui.icons.AllIconsKeys

@Composable
fun RowWithMargin(
    modifier: Modifier = Modifier,
    color: Color = Color(0xFF565660),
    leftMargin: Dp = 16.dp,
    content: @Composable () -> Unit = {}
) {
    Row(
        modifier = modifier
            .padding(start = leftMargin / 2)
            .drawBehind {
                drawLine(
                    color = color,
                    strokeWidth = 1.dp.toPx(),
                    start = center.copy(x = 0f, y = 0f),
                    end = center.copy(x = 0f, y = size.height),
                )
            }
    ) {
        Spacer(Modifier.width(width = leftMargin / 2))
        content()
    }
}

@Composable
fun CollapsableContent(
    modifier: Modifier = Modifier,
    header: @Composable (isExpanded: Boolean) -> Unit,
    content: @Composable () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                key = if (expanded) AllIconsKeys.General.ChevronDown else AllIconsKeys.General.ChevronRight,
                contentDescription = null,
//                modifier = Modifier.padding(4.dp)
            )

            header(expanded)
        }

        AnimatedVisibility(visible = expanded) {
            content()
        }
    }
}