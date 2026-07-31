package com.intellij.gwt.packaging;

import com.intellij.gwt.facet.GwtFacet;
import com.intellij.ide.util.frameworkSupport.FrameworkSupportModel;
import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.packaging.elements.PackagingElementResolvingContext;
import org.jetbrains.annotations.NotNull;

public abstract class GwtCompileOutputRelativePathSuggester {
  public static final ExtensionPointName<GwtCompileOutputRelativePathSuggester> EP_NAME = ExtensionPointName.create("com.intellij.gwt.compileOutputRelativePathSuggester");

  public abstract boolean isEnabled(FrameworkSupportModel model);

  public abstract boolean isEnabled(GwtFacet facet, PackagingElementResolvingContext context);

  public abstract @NotNull String getRelativeOutputPath();

  public static @NotNull String suggestRelativeOutputPath(FrameworkSupportModel model) {
    for (GwtCompileOutputRelativePathSuggester suggester : EP_NAME.getExtensions()) {
      if (suggester.isEnabled(model)) {
        return suggester.getRelativeOutputPath();
      }
    }
    return "/";
  }

  public static @NotNull String suggestRelativeOutputPath(GwtFacet facet, PackagingElementResolvingContext context) {
    for (GwtCompileOutputRelativePathSuggester suggester : EP_NAME.getExtensions()) {
      if (suggester.isEnabled(facet, context)) {
        return suggester.getRelativeOutputPath();
      }
    }
    return "/";
  }
}
