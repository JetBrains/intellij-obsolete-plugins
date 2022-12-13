package com.intellij.vaadin.artifact;

import com.intellij.gwt.facet.GwtFacet;
import com.intellij.gwt.packaging.GwtCompileOutputRelativePathSuggester;
import com.intellij.ide.util.frameworkSupport.FrameworkSupportModel;
import com.intellij.openapi.roots.impl.libraries.LibraryEx;
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.openapi.roots.libraries.PersistentLibraryKind;
import com.intellij.packaging.elements.PackagingElementResolvingContext;
import com.intellij.util.CommonProcessors;
import com.intellij.vaadin.framework.VaadinConstants;
import com.intellij.vaadin.framework.VaadinFrameworkType;
import com.intellij.vaadin.framework.VaadinLibraryType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jps.gwt.model.impl.sdk.GwtVaadinMavenSdkPaths;
import org.jetbrains.jps.gwt.model.impl.sdk.GwtVaadinSdkPaths;

public class VaadinWidgetSetOutputRelativePathSuggester extends GwtCompileOutputRelativePathSuggester {
  @Override
  public boolean isEnabled(FrameworkSupportModel model) {
    return model.isFrameworkSelected(VaadinFrameworkType.ID);
  }

  @Override
  public boolean isEnabled(GwtFacet facet, PackagingElementResolvingContext context) {
    String sdkType = facet.getConfiguration().getGwtSdkType();
    if (GwtVaadinSdkPaths.TYPE_ID.equals(sdkType) || GwtVaadinMavenSdkPaths.TYPE_ID.equals(sdkType)) {
      return true;
    }

    CommonProcessors.FindProcessor<Library> processor = new CommonProcessors.FindProcessor<>() {
      @Override
      protected boolean accept(Library library) {
        PersistentLibraryKind<?> kind = ((LibraryEx)library).getKind();
        return VaadinLibraryType.getInstance().getKind().equals(kind);
      }
    };
    context.getModulesProvider().getRootModel(facet.getModule()).orderEntries().forEachLibrary(processor);
    return processor.isFound();
  }

  @NotNull
  @Override
  public String getRelativeOutputPath() {
    return VaadinConstants.VAADIN_WIDGET_SETS_PATH;
  }
}
