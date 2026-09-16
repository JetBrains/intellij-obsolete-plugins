package com.intellij.bigdatatools.visualization.inlays

import com.google.gson.JsonObject
import com.intellij.bigdatatools.notebooks.core.api.nbformat.BasicNotebook
import com.intellij.bigdatatools.notebooks.core.api.nbformat.CellStatus
import com.intellij.bigdatatools.notebooks.core.api.nbformat.NotebookCell
import com.intellij.bigdatatools.notebooks.core.impl.nbformat.CellResultMessage
import com.intellij.bigdatatools.notebooks.core.impl.nbformat.CellResultType
import com.intellij.bigdatatools.notebooks.core.impl.nbformat.NotebookCellBase
import com.intellij.bigdatatools.notebooks.core.impl.nbformat.OutputCode
import com.intellij.bigdatatools.visualization.inlays.layouts.NotebookInlayLayout
import com.intellij.bigdatatools.visualization.inlays.layouts.NotebookInlayLayoutList
import com.intellij.bigdatatools.visualization.inlays.layouts.NotebookInlayLayoutSingle
import com.intellij.bigdatatools.visualization.inlays.layouts.NotebookInlayLayoutTabs
import com.intellij.bigdatatools.visualization.inlays.pages.InlayChartPage
import com.intellij.bigdatatools.visualization.inlays.pages.InlayHtmlDeferredPage
import com.intellij.bigdatatools.visualization.inlays.pages.InlayHtmlOffscreenPage
import com.intellij.bigdatatools.visualization.inlays.pages.InlayHtmlPageStub
import com.intellij.bigdatatools.visualization.inlays.pages.InlayImagePage
import com.intellij.bigdatatools.visualization.inlays.pages.InlayPage
import com.intellij.bigdatatools.visualization.inlays.pages.InlaySplitPage
import com.intellij.bigdatatools.visualization.inlays.pages.InlayTablePage
import com.intellij.bigdatatools.visualization.inlays.pages.InlayTextPage
import com.intellij.bigdatatools.visualization.inlays.style.InlaysConfig
import com.intellij.bigdatatools.visualization.inlays.style.InlaysOutputLayout
import com.intellij.bigdatatools.visualization.inlays.utils.NotebookInlayUtils
import com.intellij.bigdatatools.visualization.inlays.utils.TextToDataFrame
import com.intellij.bigdatatools.visualization.table.settings.TableSettingsZeppelinAdapter
import com.intellij.charts.core.ChartPage
import com.intellij.charts.core.axes.settings.AxesSettings
import com.intellij.charts.core.settings.ChartSettings
import com.intellij.charts.dataframe.DataFrameCSVAdapter
import com.intellij.charts.settings.SeriesSettingsPanel
import com.intellij.charts.settings.adapters.ChartSettingsZeppelinAdapter
import com.intellij.charts.utils.asJsonObjectOrNull
import com.intellij.charts.utils.getAsJsonObjectOrNull
import com.intellij.charts.utils.getAsStringOrNull
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.InvalidDataException
import com.intellij.ui.jcef.JBCefApp
import java.awt.BorderLayout

class NotebookInlayOutputController(private val inlay: NotebookInlayComponent) {

  companion object {
    private val logger = Logger.getInstance(this::class.java)
  }

  private var output: NotebookInlayLayout? = null
  val pages: List<InlayPage>
    get() = output?.getPages() ?: emptyList()

  fun hasOutput(): Boolean = output != null

  var showToolbar: Boolean = false
    set(value) {
      field = value
      output?.showToolbar = value
    }

  //fun processPossibleMarkdownCell() {
  //  if (inlay.cell.interpreterCode != "md") return
  //
  //  val output = output
  //  if (output?.getPages()?.size == 1 &&
  //      output.getFirstPage<InlayHtmlOffscreenPage>() != null &&
  //      output.getFirstPage<InlayMarkdownPage>() == null) return
  //
  //  val cellText = inlay.cell.text.removePrefix(inlay.cell.marker)
  //
  //  val markdownPage = output?.getFirstPage<InlayMarkdownPage>()
  //  if (markdownPage != null) {
  //    markdownPage.addData(cellText)
  //  }
  //  else {
  //    onOutput(inlay.cell.note, OutputCode.SUCCESS, listOf(CellResultMessage(CellResultType.MARKDOWN, cellText)), true)
  //  }
  //}

  fun detachTabs() = output?.let { inlay.panel.remove(it.component) }
  fun attachTabs() = output?.let { inlay.panel.add(it.component, BorderLayout.CENTER) }

  fun getTableDimensions(): Pair<Int, Int>? = output?.getTableDimensions()

  fun getCollapsedDescription(): String? = output?.getCollapsedDescription()

  fun removeOutput() {
    output?.let {
      inlay.panel.remove(it.component)
      Disposer.dispose(it)
      output = null
    }
  }

  private fun setOutput(layout: NotebookInlayLayout) {
    Disposer.register(inlay, layout)
    output = layout
    output?.addContentChangeListener { inlay.updateGutterComponentPosition() }

    if (!inlay.collapsed) {
      inlay.panel.add(layout.component, BorderLayout.CENTER)
      // if there was saved height, restore it.
      if (inlay.expandedHeight != 0) {
        inlay.setSize(inlay.width, inlay.expandedHeight)
        inlay.expandedHeight = 0
      }
    }

    inlay.resizeController.install()
    inlay.collapsible = true
  }

  private fun showError(errorMsg: String) {
    removeOutput()

    val page = NotebookInlayLayoutSingle().apply {
      setPages(listOf(InlayTextPage(inlay.editor.project!!, inlay.editor, inlay.cell).apply {
        addData(errorMsg)
      }), true)
    }

    removeOutput()
    setOutput(page)

    NotebookInlayUtils.doWhenPreferredSizeSet(page.component, once = true) { adjustHeight() }

    inlay.status = CellStatus.ERROR
  }

  private fun tryCreateJCEFPage(data: String): InlayHtmlOffscreenPage? {

    if (!JBCefApp.isSupported()) return null

    return try {
      InlayHtmlOffscreenPage().apply { addData(data) }
    }
    catch (e: Throwable) {
      null
    }
  }

  fun onSettingsChanged() {
    val output = output ?: return

    output.getPages().forEach { page ->
      if (page.indexInResults == -1) return@forEach

      val settingsObject = getSettingsObject(page.indexInResults)

      val tablePage = (page as? InlaySplitPage)?.tablePage ?: page as? InlayTablePage
      if (tablePage != null) {
        val tableSettings = TableSettingsZeppelinAdapter.fromJson(settingsObject)
        tableSettings?.let { tablePage.applySettings(it) }
      }

      val chartPage = (page as? InlaySplitPage)?.chartPage ?: page as? ChartPage
      if (chartPage != null) {
        val chartSettings = ChartSettingsZeppelinAdapter.fromJson(settingsObject)
        chartSettings?.let { chartPage.applySettings(it) }
      }

      (page as? InlaySplitPage)?.let {
        val newMode = if (settingsObject?.getAsStringOrNull("mode") == "table") InlaySplitPage.Mode.TABLE else InlaySplitPage.Mode.CHART
        if (it.mode != newMode) {
          it.switchMode(newMode)
        }
      }
    }
  }

  private fun saveSettings() {
    inlay.cell.note?.performModification {
      inlay.cell.config = inlay.cell.config
      (inlay.cell as NotebookCellBase).notifyChangeFields(listOf("config"))
    }
  }

  private fun getSettingsObject(outputIndex: Int): JsonObject? {
    val settings = inlay.cell.asJsonTree()

    val resultsObject = settings.getAsJsonObjectOrNull("config")?.get("results") ?: return null

    return if (resultsObject.isJsonArray) {
      val resultsJsonArray = resultsObject.asJsonArray
      if (resultsJsonArray.size() > outputIndex) {
        val result = resultsJsonArray[outputIndex].asJsonObjectOrNull()
        if (result == null) null else if (result.has("graph")) result["graph"].asJsonObjectOrNull() else result
      }
      else {
        null
      }
    }
    else if (resultsObject.isJsonObject) {

      val resultsJsonObject = resultsObject.asJsonObject

      val members = resultsJsonObject.keySet()
      if (outputIndex >= members.size) {
        return null
      }

      val iterator = members.iterator()
      var currentIndex = 0
      var finalName = ""
      while (iterator.hasNext()) {
        finalName = iterator.next()
        if (currentIndex == outputIndex)
          break
        currentIndex++
      }

      val result = resultsJsonObject[finalName].asJsonObjectOrNull()

      if (result == null) null else if (result.has("graph")) result["graph"].asJsonObjectOrNull() else result
    }
    else {
      null
    }
  }

  private fun createChartPage(cell: NotebookCell,
                              chartSettings: ChartSettings = ChartSettings(AxesSettings(), emptyList())) =
    InlayChartPage(cell, chartSettings, SeriesSettingsPanel.Mode.WITH_COLUMNS_LIST)

  fun onOutput(note: BasicNotebook?, outputCode: OutputCode, originalMsgs: List<CellResultMessage>, clear: Boolean) {

    // Visualization (fix): Fixed output duplication #BDIDE-2197
    // We think that this fix is for fixing invalid events order and should be romoved.
    //if(!clear && inlay.cell.output != null) {
    //  return
    //}

    /* It has been observed that append events
      * can be errorneously called even if paragraph
      * execution has ended, and in that case, no append
      * should be made. Also, it was observed that between PENDING
      * and RUNNING states, append-events can be called and we can't
      * miss those, else during the length of paragraph run, few
      * initial output line/s will be missing.
    */
    if (inlay.cell.status !in setOf(CellStatus.PENDING, CellStatus.RUNNING) && !clear)
      return

    if (originalMsgs.isEmpty()) {
      inlay.toolbarController.setDescription(outputCode.name)
      inlay.repaint()
      return
    }

    val pages = mutableListOf<InlayPage>()

    val msgs = NotebookInlayBokehHelper.process(originalMsgs, note)

    // ToDo NotebookInlayBokehHelper changes messages order!
    var messageIndex = 0

    msgs.forEach { msg ->

      if (msg.data.isBlank() || msg.data.contains("<strong>Output is truncated</strong>")) {
        messageIndex++
        return@forEach
      }

      when (msg.type) {

        CellResultType.TABLE -> {
          try {
            val dataFrame = DataFrameCSVAdapter.fromCsvString(msg.data, '\t')

            val settingsObject = getSettingsObject(messageIndex)

            val tableSettings = TableSettingsZeppelinAdapter.fromJson(settingsObject)
            val table = InlayTablePage(dataFrame, inlay)
            table.apply {
              indexInResults = messageIndex
              val mode = settingsObject?.getAsStringOrNull("mode")
              isDefault = mode == null || mode == "table"
              tableSettings?.let { applySettings(it) }

              addChangeListener {
                getSettingsObject(indexInResults)?.let {
                  TableSettingsZeppelinAdapter.toJson(it, getSettings())
                  saveSettings()
                }
              }

              addTabChangeListener { name ->
                getSettingsObject(indexInResults)?.let {
                  it.addProperty("mode", name)
                  saveSettings()
                }
              }
            }

            val chartSettings = ChartSettingsZeppelinAdapter.fromJson(settingsObject)
            val chart = createChartPage(inlay.cell, chartSettings ?: ChartSettings(AxesSettings(), emptyList())).apply {

              assignDataFrame(dataFrame)

              indexInResults = messageIndex

              addChangeListener {
                getSettingsObject(indexInResults)?.let {
                  ChartSettingsZeppelinAdapter.toJson(it, getSettings())
                  saveSettings()
                }
              }
            }

            pages += InlaySplitPage(table, chart)
          }
          catch (e: InvalidDataException) {
            logger.warn(e)
            showError("We encountered an error during processing table data.\n" +
                      "The possible reason could be any of special characters '\\t' or '\\n' in table cell.\n\n" +
                      (e.message ?: ""))
            return
          }
          catch (e: Exception) {
            logger.error(e)
            showError("We encountered an error during processing table data. Please, submit report.\n\n" +
                      (e.message ?: "") + "\n" + e.stackTrace.joinToString("\n"))
            return
          }
        }
        CellResultType.BOKEH_HTML -> {

          val data = msg.data

          var page: InlayPage? = try {
            InlayHtmlOffscreenPage().apply { addData(data) }
          }
          catch (e: Throwable) {
            null
          }

          if (page == null) {
            page = InlayHtmlPageStub()
          }

          val promise = (msg as CellResultMessageDelayed).promise
          if (page is InlayHtmlOffscreenPage) {
            page = InlayHtmlDeferredPage(data, promise, page)
          }

          pages += page
        }
        CellResultType.HTML, CellResultType.ANGULAR -> {

          var data = msg.data
          if (msg.type == CellResultType.ANGULAR && note != null) {
            data = NotebookInlayAngularHelper.process(data, note)
          }

          val page = NotebookInlayHtmlImageHelper.getImagePageOrNull(data) ?: tryCreateJCEFPage(data) ?: InlayHtmlPageStub()
          pages += page
        }

        CellResultType.IMG, CellResultType.SVG -> pages += InlayImagePage(msg.data)

        // ToDo DataBricks special output
        // CellResultType.MARKDOWN -> pages += InlayMarkdownPage(inlay.editor, inlay.editor.backgroundColor).apply { addData(msg.data) }

        else -> {

          var previousTextPage = pages.lastOrNull() as? InlayTextPage
          if (previousTextPage == null && !clear && pages.isEmpty()) {
            previousTextPage = output?.getPages()?.lastOrNull() as? InlayTextPage
          }

          val extractedDataframes = if (InlaysConfig.getInstance().convertTextToTable)
            TextToDataFrame.parse(msg.data,
                                  if (InlaysConfig.getInstance().specialDoubleConvertion) InlaysConfig.getInstance().specialDoubleConvertionLength
                                  else Int.MAX_VALUE)
          else emptyList()

          if (extractedDataframes.isEmpty() && !clear && previousTextPage != null) {
            previousTextPage.addData(msg.data)
          }
          else {
            if (extractedDataframes.isNotEmpty()) {
              extractedDataframes.forEach { dataFrame ->
                val table = InlayTablePage(dataFrame, inlay)
                pages += InlaySplitPage(table,
                                        createChartPage(inlay.cell).apply { assignDataFrame(dataFrame) },
                                        InlayTextPage(inlay.editor.project!!, inlay.editor, inlay.cell).apply { addData(msg.data) })
              }
            }
            else {
              pages += InlayTextPage(inlay.editor.project!!, inlay.editor, inlay.cell).apply { addData(msg.data) }
            }
          }
        }
      }

      messageIndex++
    }

    val layout = getOrCreateSuitableLayout(pages.size, clear)
    layout.setPages(pages, clear)

    NotebookInlayUtils.doWhenPreferredSizeSet(layout.component, once = false) { adjustHeight() }
    NotebookInlayUtils.doWhenUserSizeSet(layout.component, once = false) { evt ->
      inlay.setHeightLater(inlay.height + (evt.newValue as Int))
    }
    adjustHeight()
  }

  private fun getOrCreateSuitableLayout(pagesSize: Int, clear: Boolean): NotebookInlayLayout {
    val currentOutput = output

    if (currentOutput == null) {
      val newCentral = when {
        pagesSize == 1 -> NotebookInlayLayoutSingle()
        InlaysConfig.getInstance().outputLayout == InlaysOutputLayout.VERTICAL -> NotebookInlayLayoutList()
        else -> NotebookInlayLayoutTabs(inlay.editor.project!!)
      }
      setOutput(newCentral)
      return newCentral
    }

    val newOutput = if (pagesSize == 1 || pagesSize == 0) {
      if (currentOutput is NotebookInlayLayoutSingle) {
        currentOutput
      }
      else {
        if (clear) {
          NotebookInlayLayoutSingle()
        }
        else {
          val layout = when (InlaysConfig.getInstance().outputLayout) {
            InlaysOutputLayout.VERTICAL -> NotebookInlayLayoutList()
            InlaysOutputLayout.TABS -> NotebookInlayLayoutTabs(inlay.editor.project!!)
          }
          layout.setPages(currentOutput.getPages(), false)
          layout
        }
      }
    }
    else {
      when (InlaysConfig.getInstance().outputLayout) {
        InlaysOutputLayout.VERTICAL -> currentOutput as? NotebookInlayLayoutList ?: NotebookInlayLayoutList()
        InlaysOutputLayout.TABS -> currentOutput as? NotebookInlayLayoutTabs ?: NotebookInlayLayoutTabs(inlay.editor.project!!)
      }
    }

    if (newOutput != currentOutput) {
      removeOutput()
      setOutput(newOutput)
    }

    newOutput.showToolbar = showToolbar
    return newOutput
  }

  fun getPreferredHeight(): Int {
    val output = output ?: return 0
    return if (output.component.parent == null) 0 else output.component.preferredSize.height
  }

  fun adjustHeight() {

    if (inlay.userSized) {
      val minimumHeight = InlayDimensions.minHeight
      if (inlay.height < minimumHeight) {
        inlay.setHeightLater(minimumHeight)
      }
    }
    else {
      inlay.setHeightLater(getPreferredHeight() +
                           inlay.top.preferredSize.height +
                           inlay.bottom.preferredSize.height + InlayDimensions.topBorder + InlayDimensions.bottomBorder)
    }
  }

  fun updateGutter(gutter: NotebookInlayComponentGutter) {
    output?.updateGutter(gutter)
  }
}