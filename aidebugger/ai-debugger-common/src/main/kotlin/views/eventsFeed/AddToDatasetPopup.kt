package com.intellij.aidebugger.common.views.eventsFeed

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.intellij.aidebugger.common.AiDebuggerCollector
import com.intellij.aidebugger.common.onboarding.AnchorBus
import com.intellij.aidebugger.common.onboarding.OnboardingAnchorKeys
import com.intellij.aidebugger.common.onboarding.OnboardingRuntimeFlags
import com.intellij.aidebugger.common.toolWindow.LocalIdeaProject
import com.intellij.aidebugger.common.viewModels.AddToDatasetViewModel
import com.intellij.aidebugger.common.views.AIToolkitTheme
import kotlinx.coroutines.launch
import org.jetbrains.jewel.ui.component.Checkbox
import org.jetbrains.jewel.ui.component.Icon
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.jewel.ui.icons.AllIconsKeys

@Composable
fun AddToDatasetPopup(
    viewModel: AddToDatasetViewModel,
    triggerWidthPx: Int,
    triggerHeightPx: Int,
    modifier: Modifier = Modifier
) {
    val project = LocalIdeaProject.current ?: throw IllegalStateException("Project is not defined")
    val showPopup by viewModel.showPopup.collectAsState()
    val isCreatingNew by viewModel.isCreatingNew.collectAsState()
    val newName by viewModel.newDatasetName.collectAsState()
    val datasets by viewModel.datasets.collectAsState()
    val checkboxStates by viewModel.currentCheckboxStates.collectAsState()

    if (!showPopup || triggerHeightPx <= 0) return

    val density = LocalDensity.current
    val yOffsetPx = triggerHeightPx + with(density) { 8.dp.roundToPx() }

    val framework = viewModel.hierarchicalState.value?.rootEvents?.firstOrNull()?.framework

    viewModel.coroutineScope.launch {
        viewModel.showPopup.collect { show ->
            if (show && triggerHeightPx > 0) {
                AiDebuggerCollector.reportAddToDatasetPopupShown(project, framework)
            }
        }
    }

    Popup(
        onDismissRequest = { viewModel.closePopup() },
        offset = IntOffset(0, yOffsetPx),
        properties = PopupProperties(focusable = true),
    ) {
        val popupWidth = with(density) {
            if (triggerWidthPx > 0) triggerWidthPx.toDp() else 0.dp
        }

        Box(
            modifier = modifier
                .background(
                    color = AIToolkitTheme.panelBackgroundColor,
                    shape = RoundedCornerShape(6.dp)
                )
                .border(
                    width = 1.dp,
                    color = AIToolkitTheme.panelBorderColor,
                    shape = RoundedCornerShape(6.dp)
                )
                .padding(8.dp)
                .then(if (popupWidth > 0.dp) Modifier.width(popupWidth) else Modifier)
        ) {
            Column {
                // Top "New" action or text field
                if (!isCreatingNew) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clickable {
                                viewModel.startCreatingDataset()
                                try {
                                    AnchorBus.sink?.setFlag(OnboardingAnchorKeys.DATASET_CREATE_STARTED, true)
                                } catch (_: Throwable) { }
                            }
                            .padding(vertical = 4.dp)
                    ) {
                        Icon(key = AllIconsKeys.General.Add, contentDescription = null)
                        Text(text = "New Dataset", modifier = Modifier.padding(start = 6.dp))
                    }
                } else {
                    NewDatasetTextField(
                        value = newName,
                        onValueChange = { viewModel.setNewDatasetName(it) },
                        onCommit = {
                            viewModel.commitNewDataset()
                            try {
                                AnchorBus.sink?.setFlag(OnboardingAnchorKeys.ENTER_DATASET_NAME_COMMITED, true)
                            } catch (_: Throwable) { }
                                   },
                        onCancel = { viewModel.cancelNewDataset() }
                    )
                }

                // Divider-like spacing
                Box(modifier = Modifier.padding(vertical = 4.dp)) {}

                // Existing datasets with checkboxes (scrollable)
                val scrollState = rememberScrollState()
                Column(
                    modifier = Modifier
                        .heightIn(max = 240.dp)
                        .verticalScroll(scrollState)
                ) {
                    datasets.forEach { dsName ->
                        val checked = checkboxStates[dsName] ?: false
                        DatasetCheckboxRow(
                            datasetName = dsName,
                            checked = checked,
                            onCheckedChange = { isChecked ->
                                AiDebuggerCollector.reportAddToDatasetPopupCheckboxClicked(project, framework, isChecked)
                                if (isChecked) {
                                    viewModel.addToDataset(dsName) { success ->
                                        // Checkbox state is updated in ViewModel
                                        if (success) {
                                            AiDebuggerCollector.reportAddToExistingDatasetFromPopupSuccess(project, framework)
                                        } else {
                                            AiDebuggerCollector.reportAddToExistingDatasetFromPopupCancelled(project, framework)
                                        }
                                    }
                                }
                            }
                        )
                    }
                }

                // Divider-like spacing
                Box(modifier = Modifier.padding(vertical = 4.dp)) {}
            }
        }
    }
}

@Composable
private fun NewDatasetTextField(
    value: String,
    onValueChange: (String) -> Unit,
    onCommit: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    val focusRequester = remember { FocusRequester() }
    var hadFocus by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Column(modifier = modifier.padding(vertical = 4.dp)) {
        Box(modifier = Modifier.fillMaxWidth()) {
            if (value.isEmpty()) {
                Text(
                    text = "Enter dataset name",
                    modifier = Modifier.padding(start = 2.dp),
                    color = AIToolkitTheme.secondaryTextColor
                )
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = TextStyle(color = AIToolkitTheme.primaryTextColor),
                cursorBrush = SolidColor(AIToolkitTheme.primaryTextColor),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester)
                    .onFocusChanged { f ->
                        if (OnboardingRuntimeFlags.onboardingActive) return@onFocusChanged // don't close or commit on focus on onboarding
                        if (f.isFocused) {
                            hadFocus = true
                        } else if (hadFocus) {
                            if (value.trim().isNotEmpty()) {
                                onCommit()
                            } else {
                                onCancel()
                            }
                        }
                    }
                    .onPreviewKeyEvent { ke ->
                        if (ke.type == KeyEventType.KeyDown &&
                            (ke.key == Key.Enter || ke.key == Key.NumPadEnter)) {
                            onCommit()
                            true
                        } else false
                    }
            )
        }
    }
}

@Composable
private fun DatasetCheckboxRow(
    datasetName: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.padding(vertical = 4.dp)
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = { isChecked ->
                if (isChecked) {
                    onCheckedChange(true)
                }
                // Ignore uncheck attempts - keep checked state
            },
            enabled = !checked
        )
        Text(
            text = datasetName,
            modifier = Modifier.padding(start = 6.dp),
            color = if (checked) AIToolkitTheme.disabledTextColor else AIToolkitTheme.primaryTextColor
        )
    }
}
