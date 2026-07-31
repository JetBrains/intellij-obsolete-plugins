package com.intellij.gwt.packaging;

import com.intellij.gwt.GwtBundle;
import com.intellij.gwt.facet.GwtFacet;
import com.intellij.gwt.facet.GwtFacetType;
import com.intellij.openapi.project.Project;
import com.intellij.packaging.impl.elements.FacetBasedPackagingElementType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jps.gwt.model.impl.GwtExternalizationConstants;

public final class GwtCompilerOutputElementType extends FacetBasedPackagingElementType<GwtCompilerOutputElement, GwtFacet> {
  public GwtCompilerOutputElementType() {
    super(GwtExternalizationConstants.GWT_COMPILER_OUTPUT_ELEMENT_ID, GwtBundle.messagePointer("gwt.compiler.output.element.type.name"), GwtFacetType.ID);
  }

  @Override
  protected String getDialogTitle() {
    return GwtBundle.message("gwt.compiler.output.element.type.dialog.title");
  }

  @Override
  protected String getDialogDescription() {
    return GwtBundle.message("gwt.compiler.output.element.type.dialog.description");
  }

  @Override
  protected String getItemText(GwtFacet item) {
    return item.getModule().getName();
  }

  @Override
  public @NotNull GwtCompilerOutputElement createEmpty(@NotNull Project project) {
    return new GwtCompilerOutputElement(project, null);
  }

  public static GwtCompilerOutputElementType getInstance() {
    return getInstance(GwtCompilerOutputElementType.class);
  }

  @Override
  protected GwtCompilerOutputElement createElement(Project project, GwtFacet facet) {
    return new GwtCompilerOutputElement(project, facet);
  }
}
