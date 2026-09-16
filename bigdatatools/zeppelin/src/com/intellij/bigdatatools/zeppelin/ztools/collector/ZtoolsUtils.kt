package com.intellij.bigdatatools.zeppelin.ztools.collector

object ZtoolsUtils {
  fun getWords(aString: String) = aString.split(" ").filter(ZtoolsUtils::filterWord).map { it.replace("\\", "\\\\") }

  private fun filterWord(aString: String): Boolean =
    aString.isNotEmpty() && (aString.all { it.isLetterOrDigit() || it == '_' || it == '-' || it == '@' || it == '#' || it == '$' || it == '!' } || aString.startsWith(
      "`") && aString.endsWith("`"))
}