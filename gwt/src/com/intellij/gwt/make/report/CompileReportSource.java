package com.intellij.gwt.make.report;

import com.intellij.gwt.facet.GwtFacet;
import com.intellij.gwt.module.model.GwtModule;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jps.gwt.build.GwtBuilderParameters;

public class CompileReportSource {
  private final GwtFacet myFacet;
  private final GwtModule myGwtModule;

  public CompileReportSource(@NotNull GwtFacet facet, @NotNull GwtModule gwtModule) {
    myFacet = facet;
    myGwtModule = gwtModule;
  }

  public @NotNull GwtFacet getFacet() {
    return myFacet;
  }

  public @NotNull GwtModule getGwtModule() {
    return myGwtModule;
  }

  @Override
  public String toString() {
    return GwtBuilderParameters.generateCompileReportParameter(myFacet.getModule().getName(), myGwtModule.getQualifiedName());
  }
}
