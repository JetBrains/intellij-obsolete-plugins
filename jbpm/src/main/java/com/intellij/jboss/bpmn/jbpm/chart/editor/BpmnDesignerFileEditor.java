package com.intellij.jboss.bpmn.jbpm.chart.editor;

import com.intellij.diagram.DiagramProvider;
import com.intellij.jboss.bpmn.jbpm.chart.BpmnChartProvider;
import com.intellij.jboss.bpmn.jbpm.chart.model.BpmnChartSource;
import com.intellij.jboss.bpmn.jpdl.resources.messages.JpdlBundle;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.xml.DomElement;
import com.intellij.util.xml.ui.PerspectiveFileEditor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class BpmnDesignerFileEditor extends PerspectiveFileEditor {
  @NotNull final private BpmnChartSource source;
  private BpmnDesignerComponent bpmnDesignerComponent;

  protected BpmnDesignerFileEditor(Project project, VirtualFile file) {
    super(project, file);
    source = new BpmnChartSource(project, file);
    Disposer.register(this, source);
  }

  @Nullable
  @Override
  protected DomElement getSelectedDomElement() {
    return null;
  }

  @Override
  protected void setSelectedDomElement(DomElement domElement) {
  }

  @NotNull
  @Override
  protected JComponent createCustomComponent() {
    if (bpmnDesignerComponent == null) {
      bpmnDesignerComponent = createDesignerComponent();
      Disposer.register(this, bpmnDesignerComponent);
    }
    return bpmnDesignerComponent;
  }

  @Override
  public void commit() {
  }

  @Override
  public void reset() {
    bpmnDesignerComponent.scheduleUpdate();
  }

  @Nullable
  @Override
  public JComponent getPreferredFocusedComponent() {
    return null;
  }

  @Override
  @NotNull
  public String getName() {
    return JpdlBundle.message("jpdl.file.editor.designer.name");
  }

  private BpmnDesignerComponent createDesignerComponent() {
    final Ref<BpmnDesignerComponent> bpmnDesignerComponentRef = new Ref<>();
    //noinspection DialogTitleCapitalization
    ProgressManager.getInstance().runProcessWithProgressSynchronously(() -> ApplicationManager.getApplication().runReadAction(() -> {
      BpmnChartProvider bpmnChartProvider = DiagramProvider.DIAGRAM_PROVIDER.findExtension(BpmnChartProvider.class);
      BpmnDesignerComponent designerComponent = new BpmnDesignerComponent(getProject(), source, bpmnChartProvider);
      designerComponent.refreshDataModel();
      bpmnDesignerComponentRef.set(designerComponent);
    }), JpdlBundle.message("progress.manager.generating.designer.component"), false, getProject());
    return bpmnDesignerComponentRef.get();
  }
}

