package com.intellij.gwt.packaging;

import com.intellij.facet.pointers.FacetPointersManager;
import com.intellij.gwt.facet.GwtFacet;
import com.intellij.gwt.facet.GwtFacetType;
import com.intellij.openapi.roots.ui.configuration.artifacts.sourceItems.FacetBasedPackagingSourceItemsProvider;
import com.intellij.packaging.ui.ArtifactEditorContext;

public final class GwtCompilerOutputSourceItemProvider extends FacetBasedPackagingSourceItemsProvider<GwtFacet, GwtCompilerOutputElement> {
  public GwtCompilerOutputSourceItemProvider() {
    super(GwtFacetType.ID, GwtCompilerOutputElementType.getInstance());
  }

  @Override
  protected GwtCompilerOutputNodePresentation createPresentation(GwtFacet facet) {
    return GwtCompilerOutputNodePresentation
      .createOutputPresentation(FacetPointersManager.getInstance(facet.getModule().getProject()).create(facet));
  }

  @Override
  protected GwtCompilerOutputElement createElement(ArtifactEditorContext context, GwtFacet facet) {
    return new GwtCompilerOutputElement(context.getProject(), facet);
  }

  @Override
  protected GwtFacet getFacet(GwtCompilerOutputElement element) {
    return element.getFacet();
  }
}
