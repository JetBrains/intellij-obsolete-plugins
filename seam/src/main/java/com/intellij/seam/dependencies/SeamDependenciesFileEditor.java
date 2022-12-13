package com.intellij.seam.dependencies;

import com.intellij.ide.structureView.StructureViewBuilder;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorLocation;
import com.intellij.openapi.fileEditor.FileEditorState;
import com.intellij.openapi.graph.GraphManager;
import com.intellij.openapi.graph.builder.GraphBuilder;
import com.intellij.openapi.graph.builder.GraphBuilderFactory;
import com.intellij.openapi.graph.builder.components.BasicGraphComponent;
import com.intellij.openapi.graph.builder.components.GraphStructureViewBuilderSetup;
import com.intellij.openapi.graph.view.Graph2D;
import com.intellij.openapi.graph.view.Graph2DView;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.NotNullLazyValue;
import com.intellij.openapi.util.UserDataHolderBase;
import com.intellij.seam.dependencies.beans.SeamComponentNodeInfo;
import com.intellij.seam.dependencies.beans.SeamDependencyInfo;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.beans.PropertyChangeListener;

public class SeamDependenciesFileEditor extends UserDataHolderBase implements FileEditor {
  private final BasicGraphComponent myPanel;
  private final GraphBuilder<SeamComponentNodeInfo, SeamDependencyInfo> myBuilder;

  private final @NotNull NotNullLazyValue<StructureViewBuilder> myStructureViewBuilder;

  public SeamDependenciesFileEditor(final Module module) {
    final Graph2D graph = GraphManager.getGraphManager().createGraph2D();
    final Graph2DView view = GraphManager.getGraphManager().createGraph2DView();

    final SeamDependenciesDataModel model = new SeamDependenciesDataModel(module);
    final SeamDependenciesPresentationModel presentationModel = new SeamDependenciesPresentationModel(graph, module);

    myBuilder = GraphBuilderFactory.getInstance(module.getProject()).createGraphBuilder(graph, view, model, presentationModel);

    myStructureViewBuilder = NotNullLazyValue.atomicLazy(() -> GraphStructureViewBuilderSetup.setupFor(myBuilder, null));

    myPanel = new BasicGraphComponent<>(myBuilder);
    Disposer.register(this, myPanel);
  }


  @Override
  @NotNull
  public JComponent getComponent() {
    return myPanel.getComponent();
  }

  @Override
  @Nullable
  public JComponent getPreferredFocusedComponent() {
    return ((Graph2DView)myBuilder.getGraph().getCurrentView()).getJComponent();
  }

  @Override
  @NonNls
  @NotNull
  public String getName() {
    return "SeamDependenciesFileEditor";
  }

  @Override
  public void setState(@NotNull FileEditorState state) {
  }

  @Override
  public boolean isModified() {
    return false;
  }

  @Override
  public boolean isValid() {
    return true;
  }

  @Override
  public void selectNotify() {
    myBuilder.updateGraph();
  }

  @Override
  public void deselectNotify() {
  }

  @Override
  public void addPropertyChangeListener(@NotNull PropertyChangeListener listener) {
  }

  @Override
  public void removePropertyChangeListener(@NotNull PropertyChangeListener listener) {
  }

  @Override
  public FileEditorLocation getCurrentLocation() {
    throw new UnsupportedOperationException("getCurrentLocation is not implemented in : " + getClass());
  }

  @Override
  @Nullable
  public StructureViewBuilder getStructureViewBuilder() {
   return myStructureViewBuilder.getValue();
  }

  @Override
  public void dispose() {
    myBuilder.dispose();
    myPanel.dispose();
  }
}
