package com.intellij.bigdatatools.visualization.utils

import java.util.Scanner

object ResourcesUtils {
  fun getResourceAsString(resource: String): String {
    val inputStream = this::class.java.classLoader.getResourceAsStream(resource) ?: return ""
    val scanner = Scanner(inputStream).useDelimiter("\\A")
    return if (scanner.hasNext()) scanner.next() else ""
  }
}