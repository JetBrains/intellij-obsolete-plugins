package com.intellij.seam.pageflow.fileEditor;

import com.intellij.ide.structureView.StructureViewBuilder;
import com.intellij.openapi.graph.builder.components.GraphStructureViewBuilderSetup;
import com.intellij.openapi.graph.view.Graph2DView;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.NotNullLazyValue;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.xml.XmlFile;
import com.intellij.seam.resources.SeamBundle;
import com.intellij.util.xml.DomElement;
import com.intellij.util.xml.ui.PerspectiveFileEditor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.List;

public class PageflowDesignerFileEditor extends PerspectiveFileEditor {

  private PageflowDesignerComponent myComponent;
  private final XmlFile myXmlFile;

  private final @NotNull NotNullLazyValue<StructureViewBuilder> myStructureViewBuilder =
    NotNullLazyValue.atomicLazy(() -> GraphStructureViewBuilderSetup.setupFor(getPageflowDesignerComponent().getBuilder(), null));

  public PageflowDesignerFileEditor(final Project project, final VirtualFile file) {
    super(project, file);

    final PsiFile psiFile = getPsiFile();
    assert psiFile instanceof XmlFile;

    myXmlFile = (XmlFile)psiFile;
  }


  @Override
  @Nullable
  protected DomElement getSelectedDomElement() {
    final List<DomElement> selectedDomElements = getPageflowDesignerComponent().getSelectedDomElements();

    return selectedDomElements.size() > 0 ? selectedDomElements.get(0) : null;
  }

  @Override
  protected void setSelectedDomElement(final DomElement domElement) {
      getPageflowDesignerComponent().setSelectedDomElement(domElement);
  }

  @Override
  @NotNull
  protected JComponent createCustomComponent() {
    return getPageflowDesignerComponent();
  }

  @Override
  @Nullable
  public JComponent getPreferredFocusedComponent() {
   return ((Graph2DView)getPageflowDesignerComponent().getBuilder().getGraph().getCurrentView()).getJComponent();
  }

  @Override
  public void commit() {
  }

  @Override
  public void reset() {
    getPageflowDesignerComponent().getBuilder().queueUpdate();
  }

  @Override
  @NotNull
  public String getName() {
    return SeamBundle.message("seam.pageflow.designer");
  }

  @Override
  public StructureViewBuilder getStructureViewBuilder() {
    return myStructureViewBuilder.getValue();
  }

  private PageflowDesignerComponent getPageflowDesignerComponent() {
    if (myComponent == null) {
      myComponent = new PageflowDesignerComponent(myXmlFile);
      Disposer.register(this, myComponent);
    }
    return myComponent;
  }
}

