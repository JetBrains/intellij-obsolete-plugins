package com.intellij.aidebugger.common.views

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.intellij.aidebugger.common.AiDebuggerBundle
import com.intellij.aidebugger.common.services.GlobalSettingsService
import org.jetbrains.jewel.ui.component.DefaultButton
import org.jetbrains.jewel.ui.component.Text

@Composable
fun DebuggerDisabledView(modifier: Modifier = Modifier) = commonMessageView(modifier, 16.dp) {
    Text(
        fontSize = AIToolkitTheme.h3,
        text = AiDebuggerBundle.message("aitoolkit.debugger.disabled.title")
    )
    Text(text = AiDebuggerBundle.message("aitoolkit.debugger.disabled.text"))

    DefaultButton(
        onClick = { GlobalSettingsService.getInstance().enableDebugger() },
        content = {
            Text(text = AiDebuggerBundle.message("aitoolkit.debugger.disabled.button.enable"))
        }
    )
}