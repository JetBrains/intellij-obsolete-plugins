package com.intellij.gwt.packaging;

import com.intellij.gwt.facet.GwtFacet;
import com.intellij.openapi.project.Project;
import com.intellij.packaging.elements.PackagingElement;
import org.jetbrains.annotations.NotNull;

public class GwtCompilerDeployOutputElement extends GwtCompilerOutputElementBase {
  public GwtCompilerDeployOutputElement(Project project, GwtFacet facet) {
    super(GwtCompilerDeployOutputElementType.getInstance(), project, facet);
  }

  @Override
  protected GwtCompilerOutputNodePresentation createPresentation() {
    return GwtCompilerOutputNodePresentation.createDeployPresentation(myFacetPointer);
  }

  @Override
  public String toString() {
    return "gwt-deploy-output:" + myFacetPointer.getFacetName() + "(" + myFacetPointer.getModuleName() + ")";
  }

  @Override
  public boolean isEqualTo(@NotNull PackagingElement<?> element) {
    return element instanceof GwtCompilerDeployOutputElement && myFacetPointer != null
           && myFacetPointer.equals(((GwtCompilerDeployOutputElement)element).myFacetPointer);
  }
}
