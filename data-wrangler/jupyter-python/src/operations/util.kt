package com.intellij.dataWrangler.jupyterPython.operations

import com.intellij.openapi.util.text.StringUtil

val String?.pyStr: String get() =
  this?.let { "'${StringUtil.escapeCharCharacters(it)}'" } ?: "None"

