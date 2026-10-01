package com.intellij.aidebugger.common.views.components

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import com.intellij.aidebugger.common.AiDebuggerBundle
import com.intellij.aidebugger.common.views.AIToolkitTheme
import com.intellij.aidebugger.common.views.JsonFormatterTheme
import com.intellij.aidebugger.common.views.utility.annotateJson
import com.intellij.aidebugger.common.views.utility.annotateXml
import org.jetbrains.jewel.ui.component.Text


@Composable
fun PrettyText(
    annotatedText: AnnotatedString,
    maxLines: Int = 7,
    modifier: Modifier = Modifier,
    theme: JsonFormatterTheme? = null,
) {
    val theme = theme ?: AIToolkitTheme.jsonFormatterTheme

    if (annotatedText.text.isEmpty()) {
        Column {
            Text(
                "empty string",
                color = AIToolkitTheme.disabledTextColor,
                modifier = modifier,
            )
        }
        return
    }

    val lines = remember(annotatedText) { annotatedText.text.lines() }
    val shouldTruncate = lines.size > maxLines
    val truncatedLinesCount = if (shouldTruncate) lines.size - maxLines else 0

    if (shouldTruncate) {
        ShowMoreWithTextBar(
            modifier = modifier,
            content = { isExpanded ->
                val displayText = if (!isExpanded) {
                    val truncatedText = lines.take(maxLines).joinToString("\n")
                    val truncatedLength = truncatedText.length
                    buildAnnotatedString {
                        append(
                            text = annotatedText,
                            start = 0,
                            end = minOf(truncatedLength, annotatedText.length)
                        )
                    }
                } else {
                    annotatedText
                }

                Text(
                    text = displayText,
                    fontFamily = FontFamily.Monospace,
                    color = theme.textColor,
                )
            },
            moreText = {isExpanded ->
                if (isExpanded) {
                    AiDebuggerBundle.message("aitoolkit.feed.node.showLessLines")
                } else {
                    AiDebuggerBundle.message("aitoolkit.feed.node.showMoreLines", truncatedLinesCount)
                }
            }
        )
    } else {
        Text(
            text = annotatedText,
            fontFamily = FontFamily.Monospace,
            color = theme.textColor,
        )
    }
}

@Composable
fun PrettyText(
    text: String,
    maxLines: Int = 7,
    modifier: Modifier = Modifier,
    theme: JsonFormatterTheme? = null,
) {
    val theme = theme ?: AIToolkitTheme.jsonFormatterTheme

    val fullAnnotatedText = remember(text) {
        annotateJson(jsonString = text, theme = theme)
        ?: annotateXml(xmlString = text, theme = theme)
        ?: buildAnnotatedString { append(text) }
    }

    PrettyText(fullAnnotatedText, maxLines, modifier, theme)
}
