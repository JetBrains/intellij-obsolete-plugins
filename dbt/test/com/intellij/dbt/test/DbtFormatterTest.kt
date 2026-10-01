package com.intellij.dbt.test

import com.intellij.codeInsight.actions.ReformatCodeAction
import com.intellij.ide.DataManager
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnActionEvent
import java.util.concurrent.TimeUnit

class DbtFormatterTest : DbtTestCase() {
  fun testIndents() {
    myFixture.configureByFiles("formatter/model.sql")
    val originalText = myFixture.file.text

    val reformatCodeAction = ReformatCodeAction()
    val context = DataManager.getInstance().getDataContextFromFocusAsync();
    val anActionEvent = AnActionEvent(null, context.blockingGet(10, TimeUnit.SECONDS)!!,
                                      "", reformatCodeAction.getTemplatePresentation().clone(), ActionManager.getInstance(), 0)
    reformatCodeAction.actionPerformed(anActionEvent)

    myFixture.checkResult(originalText)
  }
}