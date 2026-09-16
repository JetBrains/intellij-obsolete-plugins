package com.intellij.bigdatatools.visualization.inlays.components

import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.intellij.bigdatatools.visualization.utils.VisMessagesBundle
import com.intellij.charts.dataframe.DataFrame
import com.intellij.charts.dataframe.columns.DateTimeType
import com.intellij.charts.dataframe.columns.DateType
import com.intellij.charts.dataframe.columns.DoubleArrayType
import com.intellij.charts.dataframe.columns.IntArrayType
import com.intellij.charts.dataframe.columns.IntegerType
import com.intellij.charts.dataframe.columns.LongArrayType
import com.intellij.charts.dataframe.columns.LongType
import com.intellij.charts.dataframe.columns.RealType
import com.intellij.charts.dataframe.columns.StringArrayType
import com.intellij.charts.dataframe.columns.StringType
import com.intellij.charts.dataframe.columns.TimeType
import com.intellij.charts.dataframe.columns.Type
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.EditorKind
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.editor.highlighter.EditorHighlighterFactory
import com.intellij.openapi.editor.markup.HighlighterLayer
import com.intellij.openapi.editor.markup.HighlighterTargetArea
import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.util.Disposer
import com.intellij.ui.components.JBScrollPane
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.FlowLayout
import javax.swing.Action
import javax.swing.ButtonGroup
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JRadioButton
import kotlin.math.max
import kotlin.math.min

class DDLDialog(private val project: Project, private val dataFrame: DataFrame) : DialogWrapper(null, false) {

  private val editor = EditorFactory.getInstance().createViewer(EditorFactory.getInstance().createDocument(""), project, EditorKind.PREVIEW)

  init {
    init()
    title = VisMessagesBundle.message("ddl.dialog.title")
    Disposer.register(this.disposable) {
      EditorFactory.getInstance().releaseEditor(editor)
    }
  }

  override fun createActions(): Array<Action> {
    return arrayOf(okAction)
  }

  private fun dataFrameTypeToSQL(type: Type<*>): String {
    return when (type) {
      IntegerType -> "integer"
      LongType -> "bigint"
      RealType -> "double"

      StringType -> "text"

      IntArrayType -> "integer[]"
      LongArrayType -> "bigint[]"
      DoubleArrayType -> "double[]"
      StringArrayType -> "text[]"

      DateType -> "date"
      TimeType -> "time"
      DateTimeType -> "datetime"

      else -> "unknown type"
    }
  }

  private fun setSqlDdl() {
    val keywords = mutableListOf<IntRange>()
    val variables = mutableListOf<IntRange>()

    val sb = StringBuilder()
    sb.append("CREATE TABLE data_frame").append("\n(\n")
    keywords.add(0..12)
    var addComma = false
    dataFrame.getColumns().forEach {
      if (addComma) {
        sb.append(",\n")
      }
      addComma = true
      val start = sb.length
      val dataType = dataFrameTypeToSQL(it.type)
      sb.append('\t').append(it.name).append('\t').append(dataType)

      variables.add(start + 1..start + 1 + it.name.length)
      keywords.add(start + 1 + it.name.length + 1..start + 1 + it.name.length + 1 + dataType.length)
    }

    sb.append("\n);")
    editor.document.setText(sb.toString())

    // Setting proper SQL editor.highlighter is not enough for proper highlighting, so we did it manually.
    val keywordAttributes = editor.colorsScheme.getAttributes(DefaultLanguageHighlighterColors.KEYWORD)
    keywords.forEach {
      editor.markupModel.addRangeHighlighter(it.first, it.last, HighlighterLayer.SELECTION + 1, keywordAttributes,
                                             HighlighterTargetArea.EXACT_RANGE)
    }

    val fieldAttributes = editor.colorsScheme.getAttributes(DefaultLanguageHighlighterColors.INSTANCE_FIELD)
    variables.forEach {
      editor.markupModel.addRangeHighlighter(it.first, it.last, HighlighterLayer.SELECTION + 1, fieldAttributes,
                                             HighlighterTargetArea.EXACT_RANGE)
    }
  }

  private fun getJsonDdl(): String {
    val fields = JsonArray()
    dataFrame.getColumns().forEach {
      val field = JsonObject()
      field.addProperty("name", it.name)

      when (it.type) {
        IntegerType, LongType -> field.addProperty("type", "integer")
        RealType -> field.addProperty("type", "number")

        StringType, DateType, TimeType, DateTimeType -> field.addProperty("type", "string")

        IntArrayType, LongArrayType -> {
          field.addProperty("type", "array"); field.add("items", JsonObject().apply { addProperty("type", "integer") })
        }
        DoubleArrayType -> {
          field.addProperty("type", "array"); field.add("items", JsonObject().apply { addProperty("type", "number") })
        }
        StringArrayType -> {
          field.addProperty("type", "array"); field.add("items", JsonObject().apply { addProperty("type", "string") })
        }

        else -> field.addProperty("type", "string")
      }

      fields.add(field)
    }

    val json = JsonObject().apply {
      addProperty("name", "data_frame")
      add("fields", fields)
    }

    return GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create().toJson(json)
  }

  override fun createCenterPanel(): JComponent {
    editor.settings.apply {
      isLineMarkerAreaShown = false
      isIndentGuidesShown = false
      isLineNumbersShown = false
      isFoldingOutlineShown = false
    }

    editor.setBorder(null)

    val result = editor.component

    val radioButtonSQL = JRadioButton(VisMessagesBundle.message("ddl.dialog.mode.sql"))
    val radioButtonJSON = JRadioButton(VisMessagesBundle.message("ddl.dialog.mode.json"))

    ButtonGroup().apply {
      add(radioButtonSQL)
      add(radioButtonJSON)
    }

    radioButtonSQL.addActionListener { if (radioButtonSQL.isSelected) showSchemeText(sql = true) }
    radioButtonJSON.addActionListener { if (radioButtonJSON.isSelected) showSchemeText(sql = false) }
    radioButtonSQL.isSelected = true

    showSchemeText(sql = true)

    val panel = JPanel(BorderLayout()).apply {
      add(JPanel(FlowLayout(FlowLayout.LEFT)).apply {
        add(radioButtonSQL)
        add(radioButtonJSON)
      })
    }

    val scrollPane = JBScrollPane(result).apply {
      preferredSize = Dimension(min(800, max(350, (result.preferredSize.width * 1.2).toInt())),
                                min(600, max(250, (result.preferredSize.height * 1.1).toInt())))
    }

    return JPanel(BorderLayout()).apply {
      add(panel, BorderLayout.NORTH)
      add(scrollPane, BorderLayout.CENTER)
      add(JLabel("<html>${VisMessagesBundle.message("ddl.dialog.hint")}</html>"), BorderLayout.SOUTH)
    }
  }

  private fun showSchemeText(sql: Boolean) {

    runWriteAction {
      if (!editor.isDisposed) {
        if (sql) {
          setSqlDdl()
        }
        else {
          val fileType = FileTypeManager.getInstance().getFileTypeByFileName(".json")
          (editor as EditorEx).highlighter = EditorHighlighterFactory.getInstance().createEditorHighlighter(project, fileType)
          editor.document.setText(getJsonDdl())
        }
      }
    }
  }

  override fun getDimensionServiceKey() = "com.intellij.bigdatatools.visualization.inlays.components.ddl.bounds"
}