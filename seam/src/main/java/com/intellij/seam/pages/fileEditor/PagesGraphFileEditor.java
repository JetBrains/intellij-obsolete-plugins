package com.intellij.seam.pages.fileEditor;

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

public class PagesGraphFileEditor extends PerspectiveFileEditor {

  private PagesGraphComponent myComponent;
  private final XmlFile myXmlFile;

  private final @NotNull NotNullLazyValue<StructureViewBuilder> myStructureViewBuilder =
    NotNullLazyValue.atomicLazy(() -> GraphStructureViewBuilderSetup.setupFor(getPagesGraphComponent().getBuilder(), null));

  public PagesGraphFileEditor(final Project project, final VirtualFile file) {
    super(project, file);

    final PsiFile psiFile = getPsiFile();
    assert psiFile instanceof XmlFile;

    myXmlFile = (XmlFile)psiFile;
  }


  @Override
  @Nullable
  protected DomElement getSelectedDomElement() {
    final List<DomElement> selectedDomElements = getPagesGraphComponent().getSelectedDomElements();

    return selectedDomElements.size() > 0 ? selectedDomElements.get(0) : null;
  }

  @Override
  protected void setSelectedDomElement(final DomElement domElement) {
      getPagesGraphComponent().setSelectedDomElement(domElement);
  }

  @Override
  @NotNull
  protected JComponent createCustomComponent() {
    return getPagesGraphComponent();
  }

  @Override
  @Nullable
  public JComponent getPreferredFocusedComponent() {
   return ((Graph2DView)getPagesGraphComponent().getBuilder().getGraph().getCurrentView()).getJComponent();
  }

  @Override
  public void commit() {
  }

  @Override
  public void reset() {
    getPagesGraphComponent().getBuilder().queueUpdate();
  }

  @Override
  @NotNull
  public String getName() {
    return SeamBundle.message("seam.pages.graph");
  }

  @Override
  public StructureViewBuilder getStructureViewBuilder() {
    return myStructureViewBuilder.getValue();
  }

  private PagesGraphComponent getPagesGraphComponent() {
    if (myComponent == null) {
      myComponent = new PagesGraphComponent(myXmlFile);
      Disposer.register(this, myComponent);
    }
    return myComponent;
  }
}

