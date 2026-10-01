package com.intellij.aidebugger.common.views.eventsFeed

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.intellij.aidebugger.common.views.AIToolkitTheme
import com.intellij.aidebugger.common.views.components.BoxWithToolbar
import org.jetbrains.jewel.ui.component.Icon
import org.jetbrains.jewel.ui.icon.PathIconKey
import org.jetbrains.jewel.ui.icons.AllIconsKeys

@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun EventNode(
    modifier: Modifier,
    eventIcon: PathIconKey,
    title: String,
    singleLineTitle: Boolean = false,
    defaultExpanded: Boolean = false,
    content: @Composable () -> Unit = {},
    extra: @Composable () -> Unit = {},
    children: @Composable () -> Unit = {},
    mouseOverToolBar: @Composable (() -> Unit)? = null,
) = Column(modifier = modifier) {
    CollapsableNodeItem(
        modifier = Modifier.fillMaxWidth(),
        defaultExpanded = defaultExpanded,
        title = {
            Column(
                verticalArrangement = Arrangement.spacedBy(AIToolkitTheme.smallGap)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        modifier = Modifier
                            .width(20.dp)
                            .height(16.dp),
                        key = eventIcon,
                        contentDescription = null
                    )
                    Spacer(Modifier.width(AIToolkitTheme.typicalSpace1))
                    BasicText(
                        text = title,
                        style = TextStyle(
                            fontWeight = FontWeight.Bold,
                            fontSize = AIToolkitTheme.regularTextFontSize,
                            color = AIToolkitTheme.primaryTextColor,
                        ),
                        maxLines = if (singleLineTitle) 1 else Int.MAX_VALUE,
                        overflow = if (singleLineTitle) TextOverflow.Ellipsis else TextOverflow.Clip,
                        softWrap = !singleLineTitle,
                    )

                    Spacer(Modifier.weight(1f))

                    extra()
                }
            }
        },
        content = {
            BoxWithToolbar(
                modifier = Modifier.fillMaxWidth(),
                content = content,
                mouseOverToolBar = mouseOverToolBar,
            )
        }
    )
    children()
}

@Composable
internal fun CollapsableNodeItem(
    modifier: Modifier,
    defaultExpanded: Boolean = false,
    title: @Composable () -> Unit,
    content: @Composable () -> Unit,
) {
    var isExpanded by remember { mutableStateOf(defaultExpanded) }

    var nodeModifier = modifier.background(
        color = AIToolkitTheme.panelBackgroundColor,
        shape = RoundedCornerShape(AIToolkitTheme.panelRounding)
    )

    if (isExpanded) {
        nodeModifier = nodeModifier.border(
            width = 1.dp,
            color = AIToolkitTheme.nodeBorderColor,
            shape = RoundedCornerShape(AIToolkitTheme.panelRounding)
        )
    }

    Column(
        modifier = nodeModifier
    ) {
        // HEADER
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = AIToolkitTheme.panelTitleBackgroundColor,
                    shape = RoundedCornerShape(AIToolkitTheme.panelRounding)
                ),
        ) {
            Row(
                modifier = Modifier
                    .clickable { isExpanded = !isExpanded }
                    .fillMaxWidth()
                    .padding(
                        start = AIToolkitTheme.typicalSpace1,
                        top = AIToolkitTheme.typicalSpace0,
                        end = AIToolkitTheme.typicalSpace5,
                        bottom = AIToolkitTheme.typicalSpace0,
                    ),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                title()
            }

            Box(
                modifier = Modifier
                    .size(16.dp)
                    .align(Alignment.CenterEnd)
                    .offset(x = -AIToolkitTheme.typicalSpace1),
            ) {
                Icon(
                    modifier = Modifier
                        .align(Alignment.Center),
                    key = if (isExpanded) {
                        AllIconsKeys.General.ChevronUp
                    } else {
                        AllIconsKeys.General.ChevronDown
                    },
                    contentDescription = null
                )
            }
        }
        // CONTENT
        if (isExpanded) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = AIToolkitTheme.typicalSpace1,
                        end = AIToolkitTheme.typicalSpace1,
                        top = AIToolkitTheme.typicalSpace2,
                        bottom = AIToolkitTheme.typicalSpace4
                    )
            ) {
                content()
            }
        }
    }
}
