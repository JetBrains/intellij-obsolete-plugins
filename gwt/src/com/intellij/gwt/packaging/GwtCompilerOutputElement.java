package com.intellij.gwt.packaging;

import com.intellij.gwt.facet.GwtFacet;
import com.intellij.openapi.project.Project;
import com.intellij.packaging.elements.PackagingElement;
import org.jetbrains.annotations.NotNull;

public class GwtCompilerOutputElement extends GwtCompilerOutputElementBase {

  public GwtCompilerOutputElement(Project project, GwtFacet facet) {
    super(GwtCompilerOutputElementType.getInstance(), project, facet);
  }

  @Override
  protected GwtCompilerOutputNodePresentation createPresentation() {
    return GwtCompilerOutputNodePresentation.createOutputPresentation(myFacetPointer);
  }

  @Override
  public String toString() {
    return "gwt-compiler-output:" + myFacetPointer.getFacetName() + "(" + myFacetPointer.getModuleName() + ")";
  }

  @Override
  public boolean isEqualTo(@NotNull PackagingElement<?> element) {
    return element instanceof GwtCompilerOutputElement && myFacetPointer != null
           && myFacetPointer.equals(((GwtCompilerOutputElement)element).myFacetPointer);
  }
}
