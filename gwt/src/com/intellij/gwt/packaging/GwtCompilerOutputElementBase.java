package com.intellij.gwt.packaging;

import com.intellij.facet.Facet;
import com.intellij.facet.pointers.FacetPointer;
import com.intellij.facet.pointers.FacetPointersManager;
import com.intellij.gwt.facet.GwtFacet;
import com.intellij.openapi.project.Project;
import com.intellij.packaging.elements.PackagingElement;
import com.intellij.packaging.elements.PackagingElementResolvingContext;
import com.intellij.packaging.elements.PackagingElementType;
import com.intellij.packaging.impl.elements.FacetBasedPackagingElement;
import com.intellij.packaging.impl.ui.DelegatedPackagingElementPresentation;
import com.intellij.packaging.ui.ArtifactEditorContext;
import com.intellij.packaging.ui.PackagingElementPresentation;
import com.intellij.util.xmlb.annotations.Attribute;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jps.gwt.model.impl.GwtExternalizationConstants;

public abstract class GwtCompilerOutputElementBase extends PackagingElement<GwtCompilerOutputElementBase.GwtCompilerOutputElementState>
  implements FacetBasedPackagingElement {
  protected final Project myProject;
  protected FacetPointer<GwtFacet> myFacetPointer;

  public GwtCompilerOutputElementBase(PackagingElementType type, Project project, GwtFacet facet) {
    super(type);
    myProject = project;
    myFacetPointer = facet != null ? FacetPointersManager.getInstance(project).create(facet) : null;
  }

  @Override
  public GwtCompilerOutputElementState getState() {
    final GwtCompilerOutputElementState state = new GwtCompilerOutputElementState();
    state.myFacetPointer = myFacetPointer != null ? myFacetPointer.getId() : null;
    return state;
  }

  public @Nullable GwtFacet getFacet() {
    return myFacetPointer.getFacet();
  }

  @Override
  public Facet findFacet(@NotNull PackagingElementResolvingContext context) {
    return myFacetPointer.findFacet(context.getModulesProvider(), context.getFacetsProvider());
  }

  @Override
  public void loadState(@NotNull GwtCompilerOutputElementState state) {
    final String pointer = state.myFacetPointer;
    myFacetPointer = pointer != null ? FacetPointersManager.getInstance(myProject).create(pointer) : null;
  }

  @Override
  public @NotNull PackagingElementPresentation createPresentation(@NotNull ArtifactEditorContext context) {
    return new DelegatedPackagingElementPresentation(createPresentation());
  }

  protected abstract GwtCompilerOutputNodePresentation createPresentation();

  public static class GwtCompilerOutputElementState {
    @Attribute(GwtExternalizationConstants.PACKAGING_FACET_ATTRIBUTE)
    public String myFacetPointer;
  }
}
