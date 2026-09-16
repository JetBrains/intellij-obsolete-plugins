package com.jetbrains.spark.monitoring.graph.editor

import com.intellij.diagram.DiagramBuilder
import com.intellij.diagram.DiagramBuilderFactory
import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.FileEditorPolicy
import com.intellij.openapi.fileEditor.FileEditorProvider
import com.intellij.openapi.graph.base.Node
import com.intellij.openapi.graph.view.hierarchy.HierarchyManager
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.uml.UmlFileEditorImpl
import com.jetbrains.bigdatatools.common.util.invokeLater
import com.jetbrains.spark.monitoring.graph.DotDiagramProvider

class DotEditorProvider : FileEditorProvider, DumbAware {

  override fun accept(project: Project, file: VirtualFile) = file.fileType.name == "dag-graph"

  override fun createEditor(project: Project, file: VirtualFile): FileEditor {
    val builder = WriteAction.computeAndWait<DiagramBuilder, RuntimeException> {
      DiagramBuilderFactory.getInstance().create(project, DotDiagramProvider, null, file)
    }

    // Disabling closing group in hierarchyManager
    val hierarchyManager = builder.graph.hierarchyManager
    builder.graphBuilder.hierarchyManager = object : HierarchyManager by hierarchyManager {
      override fun closeGroup(_node: Node?) = Unit
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
    private const val PROVIDER_ID = "dot-graph-editor"
  }
}