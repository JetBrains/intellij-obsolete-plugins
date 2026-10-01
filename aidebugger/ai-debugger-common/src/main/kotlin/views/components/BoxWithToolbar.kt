package com.intellij.aidebugger.common.views.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp
import com.intellij.aidebugger.common.views.AIToolkitTheme
import org.jetbrains.jewel.foundation.modifier.onHover

@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun BoxWithToolbar(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit = {},
    mouseOverToolBar: @Composable (() -> Unit)? = null,
    toolbarOffset: Offset = Offset.Zero,
) {
    var panelPointerOver by remember { mutableStateOf(false) }
    var toolBarPointerOver by remember { mutableStateOf(false) }
    val pointerOver = panelPointerOver || toolBarPointerOver

    Box(modifier = modifier
        .onHover { panelPointerOver = it }
    ) {
        content()

        if ((pointerOver) && mouseOverToolBar != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = toolbarOffset.x.dp, y = toolbarOffset.y.dp)
            ) {
                Box(modifier = Modifier
                    .background(color = AIToolkitTheme.panelBackgroundColor, shape = RoundedCornerShape(4.dp))
                    .border(width = 1.dp, color = AIToolkitTheme.panelBorderColor, shape = RoundedCornerShape(4.dp))
                    .padding(horizontal = 4.dp)
                    .onHover { toolBarPointerOver = it }
                ) {
                    mouseOverToolBar()
                }
            }
        }
    }
}