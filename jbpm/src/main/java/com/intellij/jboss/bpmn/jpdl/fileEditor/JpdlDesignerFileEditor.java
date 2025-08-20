package com.intellij.jboss.bpmn.jpdl.fileEditor;

import com.intellij.ide.structureView.StructureViewBuilder;
import com.intellij.jboss.bpmn.jpdl.resources.messages.JpdlBundle;
import com.intellij.openapi.graph.builder.components.GraphStructureViewBuilderSetup;
import com.intellij.openapi.graph.view.Graph2DView;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.NotNullLazyValue;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.xml.XmlFile;
import com.intellij.util.xml.DomElement;
import com.intellij.util.xml.ui.PerspectiveFileEditor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.List;

public class JpdlDesignerFileEditor extends PerspectiveFileEditor {

  private final XmlFile myXmlFile;
  private JpdlDesignerComponent myComponent;

  private final @NotNull NotNullLazyValue<StructureViewBuilder> myStructureViewBuilder =
    NotNullLazyValue.atomicLazy(() -> GraphStructureViewBuilderSetup.setupFor(getJpdlDesignerComponent().getBuilder(), null));

  public JpdlDesignerFileEditor(final Project project, final VirtualFile file) {
    super(project, file);

    final PsiFile psiFile = getPsiFile();
    assert psiFile instanceof XmlFile;

    myXmlFile = (XmlFile)psiFile;
  }


  @Override
  @Nullable
  protected DomElement getSelectedDomElement() {
    final List<DomElement> selectedDomElements = getJpdlDesignerComponent().getSelectedDomElements();

    return selectedDomElements.size() > 0 ? selectedDomElements.get(0) : null;
  }

  @Override
  protected void setSelectedDomElement(final DomElement domElement) {
    getJpdlDesignerComponent().setSelectedDomElement(domElement);
  }

  @Override
  @NotNull
  protected JComponent createCustomComponent() {
    return getJpdlDesignerComponent();
  }

  @Override
  @Nullable
  public JComponent getPreferredFocusedComponent() {
    return ((Graph2DView)getJpdlDesignerComponent().getBuilder().getGraph().getCurrentView()).getJComponent();
  }

  @Override
  public void commit() {
  }

  @Override
  public void reset() {
    getJpdlDesignerComponent().getBuilder().queueUpdate();
  }

  @Override
  @NotNull
  public String getName() {
    return JpdlBundle.message("jpdl.file.editor.designer.name");
  }

  @Override
  public StructureViewBuilder getStructureViewBuilder() {
    return myStructureViewBuilder.getValue();
  }

  private JpdlDesignerComponent getJpdlDesignerComponent() {
    if (myComponent == null) {
      final JpdlDesignerComponent[] graphComponent = {null};
      ProgressManager.getInstance().runProcessWithProgressSynchronously(
        (Runnable)() -> graphComponent[0] = new JpdlDesignerComponent(myXmlFile),
        JpdlBundle.message("progress.manager.generating.designer.component"), false, myXmlFile.getProject());


      myComponent = graphComponent[0];
      Disposer.register(this, myComponent);
    }
    return myComponent;
  }
}

