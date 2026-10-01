package com.intellij.aidebugger.common.views.eventsFeed

import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.unit.sp
import com.intellij.aidebugger.common.AiDebuggerBundle
import com.intellij.aidebugger.common.services.GlobalSettingsService
import com.intellij.aidebugger.common.services.LifecycleService
import com.intellij.aidebugger.common.toolWindow.LocalIdeaProject
import com.intellij.aidebugger.common.views.AIToolkitTheme
import com.intellij.aidebugger.common.views.commonMessageView
import com.intellij.aidebugger.common.views.components.HtmlText
import com.intellij.ide.DataManager
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionPlaces
import com.intellij.openapi.actionSystem.ActionUiKind
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.IdeActions
import com.intellij.openapi.actionSystem.ex.ActionUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.wm.IdeFocusManager
import org.jetbrains.jewel.ui.component.ActionButton
import org.jetbrains.jewel.ui.component.Icon
import org.jetbrains.jewel.ui.icons.AllIconsKeys

private fun disableDebugger() {
    val result = Messages.showDialog(
        null,
        AiDebuggerBundle.message("aitoolkit.debugger.settings.disable.warning"),
        AiDebuggerBundle.message("aitoolkit.debugger.settings.disable.title"),
        arrayOf(
            AiDebuggerBundle.message("aitoolkit.debugger.settings.disable.button"),
            AiDebuggerBundle.message("aitoolkit.debugger.settings.disable.cancel"),
        ),
        0,
        Messages.getQuestionIcon()
    )

    if (result == 0) {
        GlobalSettingsService.getInstance().disableDebugger()
    }
}

fun triggerRunAction(project: Project) {
    val runAction = ActionManager.getInstance().getAction(IdeActions.ACTION_DEFAULT_RUNNER)

    val focused = IdeFocusManager.getInstance(project).focusOwner
    val dataContext = DataManager.getInstance().getDataContext(focused)

    val event = AnActionEvent.createEvent(runAction, dataContext, null, ActionPlaces.UNKNOWN, ActionUiKind.NONE, null)

    ActionUtil.performAction(runAction, event)
}

@Composable
fun eventsFeedEmpty(modifier: Modifier = Modifier) = commonMessageView(modifier) {
    val project = LocalIdeaProject.current ?: return@commonMessageView
    val emptyFeedMessage by LifecycleService.getInstance(project).aiDebuggerEmptyFeedMessage.collectAsState()

    HtmlText(
        color = AIToolkitTheme.primaryTextColor,
        html = emptyFeedMessage,
        inlineContent = mapOf(
            "Run" to InlineTextContent(
                Placeholder(
                    width = 24.sp,
                    height = 24.sp,
                    placeholderVerticalAlign = PlaceholderVerticalAlign.TextCenter
                )
            ) {
                ActionButton(onClick = {
                    triggerRunAction(project)
                }) {
                    Icon(
                        AllIconsKeys.Actions.Execute,
                        contentDescription = "Run",
                        modifier,
                    )
                }

            }
        )
    )

    HtmlText(
        color = AIToolkitTheme.secondaryTextColor,
        html = AiDebuggerBundle.message("aitoolkit.feed.empty.text2"),
        links = mapOf(
            "tracing:disable" to {
                disableDebugger()
            },
        )
    )
}
