package com.jetbrains.bigdatatools.flink.graph.editor

import com.intellij.diagram.DiagramBuilder
import com.intellij.diagram.DiagramBuilderFactory
import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.FileEditorPolicy
import com.intellij.openapi.fileEditor.FileEditorProvider
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.uml.UmlFileEditorImpl
import com.jetbrains.bigdatatools.common.util.invokeLater
import com.jetbrains.bigdatatools.flink.graph.FlinkDiagramProvider

class FlinkEditorProvider : FileEditorProvider, DumbAware {

  override fun accept(project: Project, file: VirtualFile) = file.fileType.name == "flink-graph"

  override fun createEditor(project: Project, file: VirtualFile): FileEditor {
    val builder = WriteAction.computeAndWait<DiagramBuilder, RuntimeException> {
      DiagramBuilderFactory.getInstance().create(project, FlinkDiagramProvider, null, file)
    }

    invokeLater {
      builder.initialize()
      builder.queryUpdate().withRelayout().run()
      builder.view.fitContent()
    }
    return UmlFileEditorImpl(builder, file)
  }

  override fun getEditorTypeId(): String = PROVIDER_ID

  override fun getPolicy(): FileEditorPolicy = FileEditorPolicy.HIDE_DEFAULT_EDITOR

  companion object {
    private const val PROVIDER_ID = "flink-graph-editor"
  }
}