package com.intellij.aidebugger.python.extensions

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.intellij.aidebugger.common.viewModels.dataToVM
import com.intellij.aidebugger.common.views.AIToolkitTheme
import com.intellij.aidebugger.common.views.components.PrettyTextWithTools
import com.intellij.aidebugger.common.views.components.dataValueComponentSelector
import com.intellij.aidebugger.common.views.modifiers.bottomBorder
import com.intellij.aidebugger.common.views.modifiers.leftBorder
import com.intellij.aidebugger.common.views.utility.parseHtmlToAnnotatedString
import org.jetbrains.jewel.ui.component.Text

@Composable
fun LangChainPromptTemplateView(viewModel: LangChainPromptTemplateViewModel, modifier: Modifier = Modifier) {
    Column {
        LangChainPrettyKeyValueTable("inputs", viewModel.inputs, modifier)
        Spacer(modifier.size(10.dp))
        Box(
            modifier = modifier
                .fillMaxWidth()
                .border(width = 1.dp, color = Color(0xFF565660), shape = RoundedCornerShape(4.dp))
        ) {
            val inputsShape = RoundedCornerShape(4.dp)
            Column {
                Row(
                    modifier = modifier
                        .fillMaxWidth()
                        .background(
                            color = AIToolkitTheme.toolsNameBackgroundColor,
                            shape = inputsShape
                        )
                        .bottomBorder(color = AIToolkitTheme.toolsTableBorderColor)
                        .padding(6.dp)
                ) {
                    Text("outputs")
                }
                val annotatedText = viewModel.outputTextHTML?.let { parseHtmlToAnnotatedString(it) }
                PrettyTextWithTools(
                    modifier = modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    text = viewModel.outputText,
                    annotatedText = annotatedText,
                    maxLines = 5,
                )
            }
        }
    }
}

@Composable
fun LangChainPrettyKeyValueTable(headerName: String? = null, map: Map<*, *>, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .border(width = 1.dp, color = Color(0xFF565660), shape = RoundedCornerShape(4.dp))
    ) {
        Column(
            modifier = modifier.fillMaxWidth()
        ) {
            val inputsShape = RoundedCornerShape(4.dp)

            if (headerName != null) {
                Row(
                    modifier = modifier
                        .fillMaxWidth()
                        .background(
                            color = AIToolkitTheme.toolsNameBackgroundColor,
                            shape = inputsShape
                        )
                        .bottomBorder(color = AIToolkitTheme.toolsTableBorderColor)
                        .padding(6.dp)
                ) {
                    Text(headerName)
                }
            }

            map.forEach { argument ->
                if (argument.key !is String) return@forEach

                Row(
                    modifier = modifier
                        .height(IntrinsicSize.Min)
                        .bottomBorder(color = AIToolkitTheme.toolsTableBorderColor),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // KEY cell
                    Box(
                        modifier = modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .padding(6.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Text(text = argument.key as String)
                    }

                    // VALUE cell
                    Box(
                        modifier = modifier
                            .weight(3f)
                            .fillMaxHeight()
                            .leftBorder(AIToolkitTheme.toolsTableBorderColor)
                            .padding(6.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        if (argument.value is String && !(argument.value as String).isEmpty()) {
                            PrettyTextWithTools(
                                modifier = modifier.fillMaxWidth(),
                                text = argument.value as String,
                                maxLines = 2
                            )
                        } else {
                            Column(modifier = modifier) {
                                dataValueComponentSelector(dataToVM(argument.value), modifier)
                            }
                        }
                    }
                }
            }
        }
    }
}