package com.intellij.aidebugger.common.views.components

import androidx.compose.foundation.layout.heightIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.intellij.aidebugger.common.views.JsonFormatterTheme
import com.intellij.openapi.ide.CopyPasteManager
import org.jetbrains.jewel.ui.component.Icon
import org.jetbrains.jewel.ui.component.IconButton
import org.jetbrains.jewel.ui.icons.AllIconsKeys
import java.awt.datatransfer.StringSelection

@Composable
fun PrettyTextWithTools(
    text: String,
    annotatedText: AnnotatedString? = null,
    maxLines: Int = 7,
    modifier: Modifier = Modifier,
    theme: JsonFormatterTheme? = null,
) {
    val toolBar = @Composable {
        IconButton(
            onClick = {
                val selection = StringSelection(text)
                CopyPasteManager.getInstance().setContents(selection)
            },
        ) {
            Icon(
                key = AllIconsKeys.Actions.Copy,
                contentDescription = null
            )
        }
    }

    BoxWithToolbar(
        modifier = modifier.heightIn(min = 24.dp),
        content = {
            if (annotatedText == null) {
                PrettyText(text = text, maxLines = maxLines, theme = theme)
            } else {
                PrettyText(annotatedText = annotatedText, maxLines = maxLines, theme = theme)
            }
        },
        mouseOverToolBar = toolBar
    )
}
