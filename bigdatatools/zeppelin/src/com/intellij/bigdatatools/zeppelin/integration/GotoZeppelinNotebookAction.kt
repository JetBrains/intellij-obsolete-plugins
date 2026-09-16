package com.intellij.bigdatatools.zeppelin.integration

import com.intellij.ide.actions.SearchEverywhereBaseAction
import com.intellij.ide.actions.searcheverywhere.statistics.SearchFieldStatisticsCollector.wrapEventWithActionStartData
import com.intellij.openapi.actionSystem.AnActionEvent

class GotoZeppelinNotebookAction : SearchEverywhereBaseAction() {
  override fun actionPerformed(e: AnActionEvent) {
    showInSearchEverywherePopup(ZeppelinEverywhereContributor.CONTRIBUTOR_ID, wrapEventWithActionStartData(e), true, false)
  }
}