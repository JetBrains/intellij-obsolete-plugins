package com.intellij.aidebugger.common.views.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.intellij.aidebugger.common.views.AIToolkitTheme
import org.jetbrains.jewel.ui.component.Text

@Composable
fun StructuredDataView(data: Any?, modifier: Modifier = Modifier) {
    val jsonTheme = AIToolkitTheme.jsonFormatterTheme

    when(data) {
        is String -> Text(
            text = data,
            color = jsonTheme.stringColor,
            modifier = modifier
        )
        is Int,
        is Float,
        is Double -> Text(
            text = data.toString(),
            color = jsonTheme.numberColor,
            modifier = modifier
        )
        is Boolean -> Text(
            text = data.toString(),
            color = jsonTheme.boolColor,
            modifier = modifier
        )
        is List<*> -> ListDataView(data, Modifier)
        is Map<*, *> -> {
            @Suppress("UNCHECKED_CAST")
            val data = data as? Map<String, Any?> ?: throw IllegalArgumentException("Unsupported data type: ${data::class.simpleName}")
            MapDataView(data, Modifier)
        }
        else -> Text(
            text = data.toString(),
            color = jsonTheme.textColor,
        )
    }
}

@Composable
fun ListDataView(data: List<Any?>, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        data.forEachIndexed { index, item ->
            Spacer(Modifier.height(10.dp))
            Row {
                StructuredFieldView(
                    name = index.toString(),
                    data = item,
                    modifier = Modifier
                )
            }
        }
    }
}

@Composable
internal fun MapDataView(data: Map<String, Any?>, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        val entries = data.entries.toList()
        entries.forEach { entry ->
            Spacer(Modifier.height(10.dp))
            Row {
                StructuredFieldView(
                    name = entry.key,
                    data = entry.value,
                    modifier = Modifier
                )
            }
        }
    }
}

@Composable
internal fun StructuredFieldView(name: String, data: Any?, modifier: Modifier = Modifier) {
    val jsonTheme = AIToolkitTheme.jsonFormatterTheme

    if (isCollection(data)) {
        Column(modifier = modifier) {
            CollapsableContent(
                modifier = Modifier,
                header = { isExpanded ->
                    val collectionSuffix = if (isExpanded) "" else when (data) {
                        is List<*> -> "[ ${data.size} ]"
                        is Map<*, *> -> "{ ${data.size} }"
                        else -> ""
                    }

                    Row {
                        Text(
                            text = "$name:",
                            color = jsonTheme.keyColor
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = collectionSuffix,
                            color = jsonTheme.punctColor
                        )
                    }
                },
                content = {
                    RowWithMargin {
                        StructuredDataView(data = data)
                    }
                }
            )
        }
    } else {
        Row(modifier = modifier) {
            Spacer(Modifier.width(16.dp))
            Text(
                text = "$name: ",
                color = jsonTheme.keyColor
            )
            StructuredDataView(data = data)
        }
    }
}

internal fun isCollection(data: Any?): Boolean = data is List<*> || data is Map<*, *>