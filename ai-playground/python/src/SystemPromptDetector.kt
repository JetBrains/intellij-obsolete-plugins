package com.intellij.aiplayground.python

import com.intellij.openapi.project.DumbService
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import com.jetbrains.python.psi.PyAssignmentStatement
import com.jetbrains.python.psi.PyCallExpression
import com.jetbrains.python.psi.PyClass
import com.jetbrains.python.psi.PyDictLiteralExpression
import com.jetbrains.python.psi.PyExpression
import com.jetbrains.python.psi.PyFunction
import com.jetbrains.python.psi.PyListLiteralExpression
import com.jetbrains.python.psi.PyReferenceExpression
import com.jetbrains.python.psi.PyStringLiteralExpression
import com.jetbrains.python.psi.PyTargetExpression
import com.jetbrains.python.psi.PyTupleExpression

/**
 * Detects system prompts related to a given user prompt element.
 *
 * Walks up the PSI tree from the user prompt through progressively wider scopes
 * (list → call → function → class → file). At each scope, collects all system prompt
 * candidates and returns the one nearest above the user prompt.
 */
object SystemPromptDetector {
  private val SYSTEM_PROMPT_VAR_NAMES = setOf("system_prompt", "system_message", "sys_prompt")

  private data class Candidate(val value: String, val offset: Int)

  fun detectSystemPrompt(userPromptElement: PsiElement): String? {
    val userOffset = userPromptElement.textRange.startOffset
    var scope = userPromptElement.parent
    // Walk from narrow to wide scopes. A match at a narrow structural scope
    // (same list, same call) always wins over a wider scope (function, file),
    // ensuring structural relationship has priority over text proximity.
    while (scope != null) {
      if (isScopeBoundary(scope)) {
        findNearestSystemPrompt(scope, userOffset)?.let { return it }
      }
      scope = scope.parent
    }
    return null
  }

  private fun isScopeBoundary(element: PsiElement): Boolean {
    return element is PyListLiteralExpression
           || element is PyTupleExpression
           || element is PyCallExpression
           || element is PyFunction
           || element is PyClass
           || element is PsiFile
  }

  /**
   * Collects all system prompt candidates in the scope and returns the one
   * nearest before [userOffset]. If none found before, returns the nearest after.
   */
  private fun findNearestSystemPrompt(scope: PsiElement, userOffset: Int): String? {
    val candidates = mutableListOf<Candidate>()
    collectOpenAiSystemRole(scope, candidates)
    collectSystemMessageCall(scope, candidates)
    collectSystemPromptKwarg(scope, candidates)
    collectChatMessageFromSystem(scope, candidates)
    collectSystemPromptVariable(scope, candidates)
    if (candidates.isEmpty()) return null

    // Prefer the nearest candidate before the user prompt; fall back to nearest after.
    return (candidates.filter { it.offset < userOffset }.maxByOrNull { it.offset }
            ?: candidates.minByOrNull { it.offset })?.value
  }

  private fun resolveStringValue(expr: PyExpression?): String? {
    if (expr is PyStringLiteralExpression) {
      return expr.stringValue
    }
    if (expr is PyReferenceExpression) {
      val project = expr.project
      if (DumbService.isDumb(project)) return null
      val target = expr.reference.resolve() as? PyTargetExpression ?: return null
      return (target.findAssignedValue() as? PyStringLiteralExpression)?.stringValue
    }
    return null
  }

  /**
   * OpenAI / HuggingFace: {"role": "system", "content": "..."}
   */
  private fun collectOpenAiSystemRole(scope: PsiElement, candidates: MutableList<Candidate>) {
    for (dict in PsiTreeUtil.findChildrenOfType(scope, PyDictLiteralExpression::class.java)) {
      var isSystemRole = false
      var contentExpr: PyExpression? = null
      for (kv in dict.elements) {
        val key = (kv.key as? PyStringLiteralExpression)?.stringValue ?: continue
        if (key == "role" && (kv.value as? PyStringLiteralExpression)?.stringValue == "system") {
          isSystemRole = true
        }
        if (key == "content") {
          contentExpr = kv.value
        }
      }
      if (isSystemRole) {
        resolveStringValue(contentExpr)?.let { candidates.add(Candidate(it, dict.textRange.startOffset)) }
      }
    }
  }

  /**
   * LangChain: SystemMessage("...") or SystemMessage(content="...")
   */
  private fun collectSystemMessageCall(scope: PsiElement, candidates: MutableList<Candidate>) {
    for (call in PsiTreeUtil.findChildrenOfType(scope, PyCallExpression::class.java)) {
      val callee = call.callee as? PyReferenceExpression ?: continue
      if (callee.name != "SystemMessage") continue
      val contentArg = call.getKeywordArgument("content")
      val value = if (contentArg != null) resolveStringValue(contentArg) else resolveStringValue(call.arguments.firstOrNull())
      if (value != null) {
        candidates.add(Candidate(value, call.textRange.startOffset))
      }
    }
  }

  /**
   * LangChain/LlamaIndex: system_prompt="..." keyword argument in any call.
   */
  private fun collectSystemPromptKwarg(scope: PsiElement, candidates: MutableList<Candidate>) {
    for (call in PsiTreeUtil.findChildrenOfType(scope, PyCallExpression::class.java)) {
      for (name in SYSTEM_PROMPT_VAR_NAMES) {
        val kwarg = call.getKeywordArgument(name)
        if (kwarg != null) {
          resolveStringValue(kwarg)?.let { candidates.add(Candidate(it, call.textRange.startOffset)) }
        }
      }
    }
  }

  /**
   * Haystack: ChatMessage.from_system("...")
   */
  private fun collectChatMessageFromSystem(scope: PsiElement, candidates: MutableList<Candidate>) {
    for (call in PsiTreeUtil.findChildrenOfType(scope, PyCallExpression::class.java)) {
      val callee = call.callee as? PyReferenceExpression ?: continue
      if (callee.name != "from_system") continue
      val qualifier = callee.qualifier as? PyReferenceExpression ?: continue
      if (qualifier.name != "ChatMessage") continue
      val firstArg = call.arguments.firstOrNull()
      resolveStringValue(firstArg)?.let { candidates.add(Candidate(it, call.textRange.startOffset)) }
    }
  }

  /**
   * Variable assignment: system_prompt = "..."
   */
  private fun collectSystemPromptVariable(scope: PsiElement, candidates: MutableList<Candidate>) {
    for (assignment in PsiTreeUtil.findChildrenOfType(scope, PyAssignmentStatement::class.java)) {
      val target = assignment.targets.firstOrNull() as? PyTargetExpression ?: continue
      if (target.name !in SYSTEM_PROMPT_VAR_NAMES) continue
      resolveStringValue(assignment.assignedValue)?.let {
        candidates.add(Candidate(it, assignment.textRange.startOffset))
      }
    }
  }
}
