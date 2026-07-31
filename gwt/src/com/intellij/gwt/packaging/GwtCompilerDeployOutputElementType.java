package com.intellij.gwt.packaging;

import com.intellij.gwt.GwtBundle;
import com.intellij.gwt.facet.GwtFacet;
import com.intellij.gwt.facet.GwtFacetType;
import com.intellij.openapi.project.Project;
import com.intellij.packaging.impl.elements.FacetBasedPackagingElementType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jps.gwt.model.impl.GwtExternalizationConstants;

public final class GwtCompilerDeployOutputElementType extends FacetBasedPackagingElementType<GwtCompilerDeployOutputElement, GwtFacet> {
  public GwtCompilerDeployOutputElementType() {
    super(GwtExternalizationConstants.GWT_COMPILER_DEPLOY_OUTPUT_ELEMENT_ID,
          GwtBundle.messagePointer("gwt.compiler.deploy.output.element.type.name"),
          GwtFacetType.ID);
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
  public @NotNull GwtCompilerDeployOutputElement createEmpty(@NotNull Project project) {
    return new GwtCompilerDeployOutputElement(project, null);
  }

  public static GwtCompilerDeployOutputElementType getInstance() {
    return getInstance(GwtCompilerDeployOutputElementType.class);
  }

  @Override
  protected GwtCompilerDeployOutputElement createElement(Project project, GwtFacet facet) {
    return new GwtCompilerDeployOutputElement(project, facet);
  }
}
