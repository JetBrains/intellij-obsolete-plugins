package com.intellij.aiplayground.ui.chat.actions

import com.intellij.aiplayground.models.utils.AiPlaygroundCoroutine
import com.intellij.aiplayground.ui.chat.ChatViewModel
import com.intellij.aiplayground.ui.chat.ModelsListPopupStep
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.ui.popup.ListPopup
import com.intellij.ui.popup.AbstractPopup

fun showModelsPopup(project: Project, viewModel: ChatViewModel, showPopup: (ListPopup) -> Unit) {
  val coroutineScope = service<AiPlaygroundCoroutine>().coroutineScope
  val popup = JBPopupFactory.getInstance().createListPopup(ModelsListPopupStep(project, coroutineScope, viewModel))
  (popup as? AbstractPopup)?.setSpeedSearchAlwaysShown()
  showPopup(popup)
}