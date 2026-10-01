package com.intellij.aidebugger.common.views.groupedViews

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.intellij.aidebugger.common.AiDebuggerBundle
import com.intellij.aidebugger.common.views.AIToolkitTheme
import com.intellij.aidebugger.common.views.components.HtmlText
import org.jetbrains.jewel.ui.component.Text


@Composable
fun RequirementsNotMetView(modifier: Modifier = Modifier, customMessage: String? = null) {
    Box(
        modifier = modifier.padding(horizontal = 32.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                fontSize = AIToolkitTheme.h3,
                text = AiDebuggerBundle.message("aitoolkit.debugger.langgraph.requirementsNotMet.title")
            )

            HtmlText(
                color = AIToolkitTheme.primaryTextColor,
                html = customMessage ?: AiDebuggerBundle.message("aitoolkit.debugger.langgraph.requirementsNotMet.text"),
            )
        }
    }
}