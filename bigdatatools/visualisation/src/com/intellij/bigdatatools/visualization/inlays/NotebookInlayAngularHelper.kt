package com.intellij.bigdatatools.visualization.inlays

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.intellij.bigdatatools.notebooks.core.api.nbformat.BasicNotebook
import com.intellij.openapi.diagnostic.Logger
import java.util.regex.Pattern

object NotebookInlayAngularHelper {

  private val logger = Logger.getInstance(this::class.java)

  private val pattern = Pattern.compile("(\\{\\{.*?}})")

  fun process(data: String, note: BasicNotebook): String {

    var result = data

    try {
      while (true) {
        val matcher = pattern.matcher(result)
        if (!matcher.find()) break

        val group = matcher.group()
        val objectName = group.substring(2, group.length - 2)
        val angularObject = getObject(objectName, note)
        result = result.replaceRange(matcher.start(), matcher.end(), "'${angularObject ?: ""}'")
      }
    }
    catch (e: Exception) {
      logger.warn(e)
    }

    return result
  }

  private fun getObject(name: String, note: BasicNotebook): String? {
    val angularObjects = note.json["angularObjects"] as? JsonObject ?: return null

    for (angularKey in angularObjects.keySet()) {
      val array = angularObjects[angularKey] as? JsonArray ?: continue
      val found = array.find { it is JsonObject && it["name"].asString == name } ?: continue
      return (found as JsonObject)["object"].asString
    }

    return null
  }
}