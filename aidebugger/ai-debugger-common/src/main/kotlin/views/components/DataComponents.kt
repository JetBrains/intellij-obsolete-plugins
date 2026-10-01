package com.intellij.aidebugger.common.views.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.intellij.aidebugger.common.viewModels.DataListVM
import com.intellij.aidebugger.common.viewModels.DataMapVM
import com.intellij.aidebugger.common.viewModels.DataNullVM
import com.intellij.aidebugger.common.viewModels.DataStringVM
import com.intellij.aidebugger.common.viewModels.ViewModelBase
import com.intellij.aidebugger.common.views.AIToolkitTheme
import org.jetbrains.jewel.ui.component.Text

fun isCollection(viewModel: ViewModelBase): Boolean =
    viewModel is DataListVM || viewModel is DataMapVM

fun isEmptyCollection(viewModel: ViewModelBase): Boolean = when (viewModel) {
    is DataListVM -> viewModel.data.isEmpty()
    is DataMapVM -> viewModel.data.isEmpty()
    else -> false
}

@Composable
fun dataValueComponentSelector(viewModel: ViewModelBase, modifier: Modifier = Modifier) {
    when (viewModel) {
        is DataListVM -> dataListView(viewModel, modifier)
        is DataMapVM -> dataMapView(viewModel, modifier)
        is DataStringVM -> dataStringView(viewModel, modifier)
        is DataNullVM -> dataNullView(viewModel, modifier)
    }
}

fun isDataValueComponent(viewModel: ViewModelBase): Boolean =
    viewModel is DataListVM || viewModel is DataMapVM || viewModel is DataStringVM || viewModel is DataNullVM

@Composable
fun dataValueView(key: String, value: ViewModelBase, modifier: Modifier = Modifier) {
    if (isCollection(value) && !isEmptyCollection(value)) {
        CollapsableContent(
            header = {
                Text(
                    text = key,
                    fontWeight = FontWeight.Bold,
                    color = AIToolkitTheme.secondaryTextColor,
                )
            },
            content = {
                Column(modifier.padding(start = 16.dp)) {
                    dataValueComponentSelector(value, modifier)
                }
            }
        )
    } else {
        Row(verticalAlignment = Alignment.Top) {
            Box(
                Modifier
                    .width(24.dp)
                    .height(24.dp)
            )
            Text(
                "$key:",
                fontWeight = FontWeight.Bold,
                color = AIToolkitTheme.secondaryTextColor,
                modifier = modifier,
            )
            dataValueComponentSelector(value, modifier)
        }
    }
}

@Composable
fun dataMapView(viewModel: DataMapVM, modifier: Modifier = Modifier) {
    if (viewModel.data.isEmpty()) {
        Column {
            Text(
                "empty dict",
                color = AIToolkitTheme.disabledTextColor,
                modifier = modifier,
            )
        }
        return
    }
    viewModel.data.forEach { (key, value) ->
        if (value !is ViewModelBase) {
            return@forEach
        }

        dataValueView(key, value, modifier)
    }
}

@Composable
fun dataListView(viewModel: DataListVM, modifier: Modifier = Modifier) {
    if (viewModel.data.isEmpty()) {
        Column {
            Text(
                "empty list",
                color = AIToolkitTheme.disabledTextColor,
                modifier = modifier,
            )
        }
        return
    }
    viewModel.data.forEachIndexed { key, value ->
        if (value !is ViewModelBase) {
            return@forEachIndexed
        }

        dataValueView(key.toString(), value, modifier)
    }
}

@Composable
fun dataStringView(viewModel: DataStringVM, modifier: Modifier = Modifier) {
    if (viewModel.data.isEmpty()) {
        Column {
            Text(
                "empty string",
                color = AIToolkitTheme.disabledTextColor,
                modifier = modifier,
            )
        }
        return
    }
    SelectionContainer(modifier = modifier) { Text(viewModel.data) }
}

@Composable
fun dataNullView(viewModel: DataNullVM, modifier: Modifier = Modifier) {
    Text(text = "null", modifier = modifier)
}