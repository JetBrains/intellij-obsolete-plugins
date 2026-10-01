package com.intellij.aiplayground.python

import com.intellij.aiplayground.models.chat.ChatRepository
import com.intellij.aiplayground.models.utils.AiPlaygroundCoroutine
import com.intellij.aiplayground.ui.chat.ChatUiProvider
import com.intellij.aiplayground.ui.utils.isChinaRegion
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.util.indexing.FileBasedIndex
import com.jetbrains.python.psi.PyPlainStringElement
import com.jetbrains.python.psi.PyReferenceExpression
import com.jetbrains.python.psi.PyStringLiteralExpression
import kotlinx.coroutines.launch

fun PsiElement.asPromptElementOrNull(mustBeFirstChild: Boolean = true): PyPlainStringElement? {
  //Bad optimization of internal code
  //Rewrite it before remove this
  //See https://youtrack.jetbrains.com/issue/PY-85199/PyCharm-freezes-on-opening-large-Jupyter-notebooks
  if (textRange.length > 10_000)
    return null

  if (isChinaRegion()) return null
  if (this !is PyPlainStringElement) return null
  val literalElement = this.parent ?: return null
  // only for the first child, otherwise multiline string literals will have multiple markers
  if (mustBeFirstChild && literalElement.firstChild != this) return null
  if (literalElement !is PyStringLiteralExpression || literalElement.isDocString) return null
  for (part in literalElement.stringElements)
    if (part.isFormatted() || part.isRaw || part.isTemplate) return null
  if (literalElement.parent is PyReferenceExpression && (literalElement.parent as PyReferenceExpression).name == "format")
    return null

  if (InjectedLanguagesDetector.anyUndesirableInjectedLanguageDetected(literalElement)) return null

  // calling the whole modified pipeline
  val detector = MLPromptStringsDetector.getInstance()
  if (!detector.detectPrompts(literalElement)) return null


  var aiImportFound = false
  FileBasedIndex.getInstance().processAllKeys(AiPlaygroundImportNameKey.NAME, { value ->
    FileBasedIndex.getInstance().processValues(AiPlaygroundImportNameKey.NAME, value, null, { _, _ ->
      aiImportFound = true
      false // return false to stop checking the remaining values from the indices
    }, GlobalSearchScope.projectScope(this.project))
    !aiImportFound
  }, literalElement.project)

  if (!aiImportFound) {
    return null
  }

  return this
}

fun openPlaygroundWithPrompt(project: Project, element: PsiElement) {
  val (chatInput, systemPrompt) = ReadAction.compute<Pair<String, String?>?, Throwable> {
    val userPrompt = (element.parent as? PyStringLiteralExpression)?.stringValue ?: return@compute null
    val sysPrompt = SystemPromptDetector.detectSystemPrompt(element)
    userPrompt to sysPrompt
  } ?: return
  service<AiPlaygroundCoroutine>().coroutineScope.launch {
    val newChat = project.service<ChatRepository>().createChat()
    val vm = project.service<ChatUiProvider>().openChat(newChat)
    vm?.updateInput(chatInput)
    if (systemPrompt != null) {
      vm?.updateSystemPromptText(systemPrompt)
    }
  }
}
