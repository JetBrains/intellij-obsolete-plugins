package com.intellij.jboss.bpmn.jbpm.model;

import com.intellij.ide.structureView.StructureViewModel;
import com.intellij.ide.structureView.StructureViewTreeElement;
import com.intellij.ide.structureView.impl.xml.XmlFileTreeElement;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.xml.XmlFile;
import com.intellij.util.Function;
import com.intellij.util.xml.*;
import com.intellij.util.xml.structure.DomStructureTreeElement;
import com.intellij.util.xml.structure.DomStructureViewBuilder;
import com.intellij.util.xml.structure.DomStructureViewTreeModel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class BpmnStructureViewBuilder extends DomStructureViewBuilder {
  private final Function<DomElement, DomService.StructureViewMode> myDescriptor;
  private final XmlFile myFile;

  public BpmnStructureViewBuilder(XmlFile file, Function<DomElement, DomService.StructureViewMode> descriptor) {
    super(file, descriptor);
    myFile = file;
    myDescriptor = descriptor;
  }

  @NotNull
  @Override
  public StructureViewModel createStructureViewModel(@Nullable Editor editor) {
    return new DomStructureViewTreeModel(myFile, myDescriptor, editor) {
      @NotNull
      @Override
      public StructureViewTreeElement getRoot() {
        final DomFileElement<DomElement> fileElement =
          DomManager.getDomManager(myFile.getProject()).getFileElement(myFile, DomElement.class);
        return fileElement == null ?
               new XmlFileTreeElement(myFile) :
               new MyStructureTreeElement(fileElement.getRootElement().createStableCopy(), myDescriptor, getNavigationProvider());
      }
    };
  }

  private static final class MyStructureTreeElement extends DomStructureTreeElement {
    @NotNull private final Function<DomElement, DomService.StructureViewMode> myDescriptor;
    @Nullable private final DomElementNavigationProvider myNavigationProvider;

    private MyStructureTreeElement(@NotNull DomElement element,
                                   @NotNull Function<DomElement, DomService.StructureViewMode> descriptor,
                                   @Nullable DomElementNavigationProvider navigationProvider) {
      super(element, descriptor, navigationProvider);
      myDescriptor = descriptor;
      myNavigationProvider = navigationProvider;
    }

    @Override
    public String getPresentableText() {
      if (!getElement().isValid()) return "<unknown>";
      final ElementPresentation presentation = getElement().getPresentation();
      final String name = presentation.getElementName();
      final String documentation = presentation.getDocumentation();
      final String typeName = presentation.getTypeName();
      StringBuilder sb = new StringBuilder();
      if (!StringUtil.isEmptyOrSpaces(typeName)) {
        sb.append(typeName);
      }
      else {
        sb.append("Unknown type");
      }
      if (!StringUtil.isEmptyOrSpaces(name)) {
        sb.append(": ");
        sb.append(name);
      }
      if (!StringUtil.isEmptyOrSpaces(documentation)) {
        sb.append(" (").append(documentation).append(')');
      }
      return sb.toString();
    }

    @Override
    protected StructureViewTreeElement createChildElement(DomElement element) {
      return new MyStructureTreeElement(element, myDescriptor, myNavigationProvider);
    }
  }
}
