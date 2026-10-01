package com.intellij.aiplayground.python

import com.intellij.psi.PsiElement
import com.jetbrains.python.psi.PyAssertStatement
import com.jetbrains.python.psi.PyCallExpression
import com.jetbrains.python.psi.PyQualifiedExpression
import com.jetbrains.python.psi.PyRaiseStatement
import com.jetbrains.python.psi.PyReferenceExpression
import com.jetbrains.python.psi.PyStringLiteralExpression


@Suppress("unused")
object PromptCandidateFilter {

  private val Q_CHARS = setOf('?', '？')
  // matches a full-line URL (HTTP/HTTPS/FTP);
  private val URL_RE = Regex("""^\s*(https?|ftp)://\S+\s*$""", RegexOption.IGNORE_CASE)
  // matches a single identifier-like token
  private val IDENT_RE = Regex("^[A-Za-z_][A-Za-z0-9_]*$")
  // matches a bare filename (no directory) with an extension
  private val FNAME_RE = Regex("""^[A-Za-z0-9_.-]+\.[A-Za-z0-9]{1,7}$""")
  // flags strings that likely represent regular expressions
  private val REGEX_RE = Regex(
    """\\\\[dwsDWS]|\[\^?.+?]|\(\??[:=!]|\(\?P<|\{[0-9]+(,[0-9]*)?}|\.\*|\.\+|\$|\^|\||\\\\[bBAZ]"""
  )

  private fun proportionAlpha(s: String): Double {
    if (s.isEmpty()) return 0.0
    val alpha = s.count { it.isLetter() }
    return alpha.toDouble() / s.length
  }

  private fun hasQ(s: String): Boolean = s.any { it in Q_CHARS }

  private fun noWhitespace(s: String): Boolean = s.none { it.isWhitespace() }

  /**
   * Keep function, first filtering step, decides whether to keep a candidate based on string properties
   * returns true to keep, false to filter out.
   */
  fun keepTextPythonParity(s: String): Boolean {
    val trimmed = s.trim()

    /**
     * This filter is meant to eliminate very short strings which are dominant in code and are very unlikely to be prompts.
     * Though there might be some rare occasions where they are indeed prompts like: Show me, Generate, etc,
     * it’s reasonable to take this risk in order to prevent model overload
     */
    if (s.length <= 12 && !hasQ(s)) return false
    /**
     * There are few realistic prompt examples that would get eliminated by this filter.
     * It once again filters out a lot of junk present in code files
     */
    if (proportionAlpha(s) < 0.4) return false

    if (URL_RE.matches(s)) return false
    if (FNAME_RE.matches(trimmed)) return false

    if ('_' in s && IDENT_RE.matches(trimmed)) return false

    if (noWhitespace(s)) return false

    if (REGEX_RE.containsMatchIn(s)) return false

    return true
  }

  /**
   * String context analyzing functions, removing obvious negatives such as:
   * strings inside assert, raise, print and log statements
   */

  private fun isUnderAssert(literal: PyStringLiteralExpression): Boolean =
    generateSequence<PsiElement>(literal) { it.parent }.any { it is PyAssertStatement }

  private fun isUnderRaise(literal: PyStringLiteralExpression): Boolean =
    generateSequence<PsiElement>(literal) { it.parent }.any { it is PyRaiseStatement }

  private val LOG_NAMES = setOf(
    "debug", "info", "warning", "warn", "error", "critical", "exception", "fatal"
  )

  private fun isInLoggingCall(literal: PyStringLiteralExpression): Boolean {
    val call = generateSequence<PsiElement>(literal) { it.parent }
                 .firstOrNull { it is PyCallExpression } as? PyCallExpression ?: return false
    val callee = call.callee
    val name = (callee as? PyQualifiedExpression)?.referencedName?.lowercase()
    return name != null && name in LOG_NAMES
  }

  private fun isInPrintCall(literal: PyStringLiteralExpression): Boolean {
    val call = generateSequence<PsiElement>(literal) { it.parent }
                 .firstOrNull { it is PyCallExpression } as? PyCallExpression ?: return false
    val calleeName = (call.callee as? PyReferenceExpression)?.referencedName
    return calleeName == "print"
  }

  /**
   * Returns true if the literal should be kept for downstream detection/ML.
   */
  fun shouldKeep(literal: PyStringLiteralExpression): Boolean {
    // Text-only keep(s)
    if (!keepTextPythonParity(literal.stringValue)) return false

    // Contextual skips
    if (isUnderAssert(literal)) return false
    if (isUnderRaise(literal)) return false
    if (isInLoggingCall(literal)) return false
    if (isInPrintCall(literal)) return false

    return true
  }
}