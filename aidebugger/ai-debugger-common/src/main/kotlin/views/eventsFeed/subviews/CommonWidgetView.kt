package com.intellij.aidebugger.common.views.eventsFeed.subviews

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.intellij.aidebugger.common.viewModels.CommonWidgetVM
import com.intellij.aidebugger.common.views.AIToolkitTheme
import org.jetbrains.jewel.ui.component.Icon
import org.jetbrains.jewel.ui.component.Text

@Composable
fun CommonWidgetView(viewModel: CommonWidgetVM, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            modifier = Modifier.padding(all = 1.dp),
            key = AIToolkitTheme.iconByPath(viewModel.iconKey),
            contentDescription = null
        )
        Spacer(Modifier.width(AIToolkitTheme.typicalSpace0))
        Text(
            color = AIToolkitTheme.secondaryTextColor,
            fontSize = AIToolkitTheme.smallerTextFontSize,
            text = viewModel.text
        )
    }
}