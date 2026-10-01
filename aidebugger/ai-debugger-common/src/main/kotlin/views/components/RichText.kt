package com.intellij.aidebugger.common.views.components


import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import com.intellij.aidebugger.common.views.AIToolkitTheme
import com.intellij.aidebugger.common.views.utility.parseHtmlToAnnotatedString
import org.jetbrains.jewel.ui.component.Text

@Composable
fun HtmlText(
    html: String, 
    modifier: Modifier = Modifier,
    color: Color = AIToolkitTheme.primaryTextColor,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip,
    links: Map<String, () -> Unit> = emptyMap(),
    inlineContent: Map<String, InlineTextContent> = emptyMap(),
) {
    val annotatedString = parseHtmlToAnnotatedString(html, links)

    Text(
        text = annotatedString,
        modifier = modifier,
        color = color,
        maxLines = maxLines,
        overflow = overflow,
        inlineContent = inlineContent,
    )
}
