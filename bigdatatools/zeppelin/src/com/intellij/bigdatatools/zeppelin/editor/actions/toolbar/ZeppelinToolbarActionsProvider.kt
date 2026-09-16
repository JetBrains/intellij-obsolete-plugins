package com.intellij.bigdatatools.zeppelin.editor.actions.toolbar

import com.intellij.bigdatatools.zeppelin.drivers.actions.ZeppelinShowInterpreterAndDependencySettings
import com.intellij.bigdatatools.zeppelin.editor.ZeppelinEditor
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.actionSystem.Separator

object ZeppelinToolbarActionsProvider {
  fun createActionsForLocal(): List<AnAction> = listOf(ZeppelinShowInterpreterAndDependencySettings())

  fun createActionsForRemote(noteEditor: ZeppelinEditor): List<AnAction> {
    val webActionGroup = DefaultActionGroup().apply {
      templatePresentation.isPopupGroup = true
      templatePresentation.icon = AllIcons.General.Web
      add(OpenInExternalBrowserAction(noteEditor))
      add(ZeppelinCopyLinkAction(noteEditor))
    }

    val extractActions = ZeppelinExtensionToolbarActionProvider.createFor(noteEditor)
    val list = if (extractActions.isNotEmpty()) listOf(Separator.create()).plus(extractActions) else emptyList()

    return createActionsForLocal() + listOf(Separator.create(), webActionGroup) + list
  }
}