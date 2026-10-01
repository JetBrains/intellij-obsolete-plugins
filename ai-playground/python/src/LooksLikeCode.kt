package com.intellij.aiplayground.python

import java.util.regex.Pattern

/**
 * Detects if a string looks like code.
 *
 * Original code taken from [com.intellij.ml.llm.intentions.conversion.language.looksLikeCode]
 * and modified to serve better for detection in string literals in code files
 */
object LooksLikeCode {
  private val id = "[a-zA-Z_$][a-zA-Z0-9_$]*"
  private val assignment = Pattern.compile("= ['\"]")
  private val idChain = Pattern.compile("$id\\.$id\\.$id")
  private val call = Pattern.compile("$id\\(.*\\)")
  private val escapeSeq = Pattern.compile("(?:\\\\[ntbru].*){2,}")
  private val csharpTuple = "($id|\\($id(, *$id)+\\))"
  private val csharpGenericTp = "(((in|out) +)?$csharpTuple)"
  private val csharpGenericM = Pattern.compile(
    "$id<$csharpGenericTp(, *$csharpGenericTp)*>\\(.*\\)"
  )
  private val scalaTodo = Pattern.compile("= ?\\?\\?\\?")
  private val javaVarDecl = Pattern.compile("\\b([A-Z])($id) ([a-z])(\\2) ?[;:,)=]")

  fun looksLikeCode(text: String): Boolean {
    if (assignment.matcher(text).find()) return true
    if (idChain.matcher(text).find()) return true
    if (call.matcher(text).find()) return true
    if (escapeSeq.matcher(text).find()) return true
    if (csharpGenericM.matcher(text).find()) return true
    if (scalaTodo.matcher(text).find()) return true
    if (javaVarDecl.matcher(text).find()) return true

    return false
  }
}