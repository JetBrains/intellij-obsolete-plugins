package com.intellij.bigdatatools.visualization.inlays

import com.intellij.bigdatatools.coreUi.util.executeOnPooledThread
import com.intellij.bigdatatools.coreUi.util.messageOrDefault
import com.intellij.bigdatatools.notebooks.core.api.nbformat.BasicNotebook
import com.intellij.bigdatatools.notebooks.core.impl.nbformat.CellResultMessage
import com.intellij.bigdatatools.notebooks.core.impl.nbformat.CellResultType
import com.intellij.bigdatatools.visualization.inlays.settings.InlaysSettings
import com.intellij.bigdatatools.visualization.utils.VisMessagesBundle
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.util.io.FileUtil
import com.intellij.util.io.HttpRequests
import com.jetbrains.bigdatatools.common.util.BdtAsyncPromise
import org.jetbrains.concurrency.AsyncPromise
import java.io.File
import java.net.URI
import java.nio.file.Paths
import java.util.concurrent.ConcurrentHashMap
import java.util.regex.Pattern

/**
 * Current JCEF implementation have multiple specialities, and cannot render all HTML properly.
 * Zeppelin Bokeh output is the one example.
 *
 * Path where downloaded js is stored:
 * Linux: ~/tmp/Bokeh-<N>downloaded
 */
object NotebookInlayBokehHelper {

  private val logger = Logger.getInstance(this::class.java)

  private val bokehDivInitPattern = Pattern.compile("^\\s*<div class=\"bk-root\">")
  private val bokehScriptPattern = Pattern.compile("^\\s*<script")
  private val bokehDivChartPattern = Pattern.compile("^\\s*<div class=\"bk-root\" id=")

  private val scriptsForNote = mutableMapOf<BasicNotebook?, MutableSet<String>>()

  // Key -  joined jsons names. Value - single string with script block prepared for inserting to html.
  private var bokehStrings = mutableMapOf<String, String>()

  // Key -  joined jsons names.
  private var promises = ConcurrentHashMap<String, AsyncPromise<String>>()

  private fun String.getFileName() = Paths.get(URI(this).path).fileName.toString()

  private fun getSync(dir: File, bokehFiles: Set<String>): String? {

    if (bokehFiles.isEmpty()) {
      logger.warn("Empty bokehFiles list")
    }

    val result = StringBuilder()

    for (bokehFile in bokehFiles) {
      try {
        val fileName = bokehFile.getFileName()
        val file = File(dir, fileName)
        if (!file.exists()) return null
        result.append("<script>").append(file.readText()).append("</script>")
      }
      catch (e: Exception) {
        return null
      }
    }
    return result.toString()
  }

  private fun getScriptsPath(): File? {
    val storedPath = InlaysSettings.getInstance().bokehScriptsPath ?: return null
    try {
      val storedDir = File(storedPath)
      return if (storedDir.exists() && storedDir.isDirectory) storedDir else null
    }
    catch (e: Exception) {
      return null
    }
  }

  private fun getOrCreateScriptsPath(): File {
    return getScriptsPath() ?: FileUtil.createTempDirectory("Bokeh-", "downloaded", false)
  }

  private fun getSync(note: BasicNotebook?): String? = synchronized(this) {

    val jsons = scriptsForNote[note]

    if (jsons == null) {
      logger.warn("Empty bokehFiles list")
      return null
    }

    val jsonsKey = jsons.joinToString { it.getFileName() }

    var bokehString = bokehStrings[jsonsKey]

    if (!bokehString.isNullOrBlank()) {
      return bokehString
    }

    val storedPath = getScriptsPath() ?: return null

    val result = getSync(storedPath, jsons)
    bokehString = if (result.isNullOrBlank()) {
      null
    }
    else {
      bokehStrings[jsonsKey] = result
      result
    }

    return bokehString
  }

  private fun getAsync(note: BasicNotebook?): AsyncPromise<String> = synchronized(this) {

    val jsons = scriptsForNote[note]?.toSet() ?: emptySet()

    if (jsons.isEmpty()) {
      logger.warn("Empty bokehFiles list")
    }

    val jsonsKey = jsons.joinToString { it.getFileName() }

    var localPromise = promises[jsonsKey]
    if (localPromise != null) {
      return localPromise
    }

    localPromise = BdtAsyncPromise()
    promises[jsonsKey] = localPromise

    val scriptsPath = getOrCreateScriptsPath()

    executeOnPooledThread {

      try {
        jsons.forEach {
          if (!File(scriptsPath, it.getFileName()).exists()) {
            val data = HttpRequests.request(it).readTimeout(30000).readBytes(null)
            File(scriptsPath, it.getFileName()).writeBytes(data)
          }
        }

        InlaysSettings.getInstance().bokehScriptsPath = scriptsPath.absolutePath

        val result = getSync(scriptsPath, jsons)
        if (result.isNullOrBlank()) {
          localPromise.setError(VisMessagesBundle.message("bokeh.loading.sync.error"))
        }
        else {
          localPromise.setResult(result)
        }
      }
      catch (e: Exception) {
        localPromise.setError(e.messageOrDefault())
      }
    }

    return localPromise
  }

  /**
   * Zeppelin returns bokeh output as 4 different html outputs.
   * First - div with span with id (placeholder for loading logo)
   * Second - JS for loading and initializing bokeh library.
   * Third - div with id (placeholder for chart).
   * Forth - chart data and initialization script.
   *
   * But this outputs could be in different cells (one cell - load and init library, other cell - chart initialization and chart data)
   *
   * We are here processing msgs and if bokeh is found, trying to join all of this 4 (or 2) part to a single message dropping JS loading messages.
   */
  fun process(msgs: List<CellResultMessage>, note: BasicNotebook?): List<CellResultMessage> {

    if (msgs.size < 2) return msgs

    var result = msgs

    var i = 0
    do {
      if (i + 3 < result.size &&
          result[i].type == CellResultType.HTML &&
          result[i + 1].type == CellResultType.HTML &&
          result[i + 2].type == CellResultType.HTML &&
          result[i + 3].type == CellResultType.HTML &&
          bokehDivInitPattern.matcher(result[i].data).find() &&
          bokehScriptPattern.matcher(result[i + 1].data).find() &&
          bokehDivChartPattern.matcher(result[i + 2].data).find() &&
          bokehScriptPattern.matcher(result[i + 3].data).find()) {

        scriptsForNote.getOrPut(note) { HashSet() }.addAll(getBokehUrls(result[i + 1].data))

        // First two div and script suits for bokeh js loading, but we are loading all js by ourselves.
        val resultMsg = createBokehResultMessage(result[i + 2].data + result[i + 3].data, note)

        result = result.subList(0, i) + resultMsg + result.subList(i + 4, result.size)
      }
      else if (result[i].type == CellResultType.HTML &&
               result[i + 1].type == CellResultType.HTML &&
               bokehDivChartPattern.matcher(result[i].data).find() &&
               bokehScriptPattern.matcher(result[i + 1].data).find()) {

        val resultMsg = createBokehResultMessage(result[i].data + result[i + 1].data, note)
        result = result.subList(0, i) + resultMsg + result.subList(i + 2, result.size)
      }
      else if (result[i].type == CellResultType.HTML &&
               result[i + 1].type == CellResultType.HTML &&
               bokehDivInitPattern.matcher(result[i].data).find() &&
               bokehScriptPattern.matcher(result[i + 1].data).find()) {

        scriptsForNote.getOrPut(note) { HashSet() }.addAll(getBokehUrls(result[i + 1].data))

        result = result.subList(0, i) + result.subList(i + 2, result.size)
      }
      i++
    }
    while (i <= result.size - 2)

    return result
  }

  private fun getBokehUrls(data: String): List<String> {
    var start = data.indexOf("var js_urls = [\"")
    if (start == -1) {
      return emptyList()
    }
    start += 14
    val end = data.indexOf("]", start)
    if (end == -1) {
      return emptyList()
    }

    val urls = data.substring(start + 1, end)
    val split = urls.split(",")

    return split.map { it.trim().removeSurrounding("\"") }
  }

  private fun createBokehResultMessage(data: String, note: BasicNotebook?): CellResultMessage {

    val allBokehJs = getSync(note)

    return if (allBokehJs == null) {
      CellResultMessageDelayed(CellResultType.BOKEH_HTML, data, getAsync(note))
    }
    else {
      CellResultMessage(CellResultType.HTML, allBokehJs + data)
    }
  }
}