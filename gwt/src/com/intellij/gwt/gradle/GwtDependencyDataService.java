package com.intellij.gwt.gradle;

import com.intellij.facet.ModifiableFacetModel;
import com.intellij.gwt.facet.GwtFacet;
import com.intellij.gwt.facet.GwtFacetConfiguration;
import com.intellij.gwt.facet.GwtFacetType;
import com.intellij.gwt.sdk.GwtSdkManager;
import com.intellij.gwt.sdk.GwtSdkType;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.externalSystem.model.DataNode;
import com.intellij.openapi.externalSystem.model.Key;
import com.intellij.openapi.externalSystem.model.ProjectKeys;
import com.intellij.openapi.externalSystem.model.project.LibraryDependencyData;
import com.intellij.openapi.externalSystem.model.project.LibraryPathType;
import com.intellij.openapi.externalSystem.model.project.ProjectData;
import com.intellij.openapi.externalSystem.service.project.IdeModifiableModelsProvider;
import com.intellij.openapi.externalSystem.service.project.manage.AbstractProjectDataService;
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil;
import com.intellij.openapi.externalSystem.util.ExternalSystemConstants;
import com.intellij.openapi.externalSystem.util.Order;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jps.gwt.model.impl.sdk.GwtGradleSdkPaths;
import org.jetbrains.plugins.gradle.util.GradleConstants;

import java.util.Collection;
import java.util.Set;

@Order(ExternalSystemConstants.UNORDERED)
public final class GwtDependencyDataService extends AbstractProjectDataService<LibraryDependencyData, Module> {
  @Override
  public @NotNull Key<LibraryDependencyData> getTargetDataKey() {
    return ProjectKeys.LIBRARY_DEPENDENCY;
  }

  @Override
  public void importData(@NotNull Collection<? extends DataNode<LibraryDependencyData>> toImport,
                         @Nullable ProjectData projectData,
                         @NotNull Project project,
                         @NotNull IdeModifiableModelsProvider modelsProvider) {
    for (DataNode<LibraryDependencyData> node : toImport) {
      String externalName = node.getData().getExternalName();
      if (externalName.startsWith("com.google.gwt:gwt-servlet:") || externalName.startsWith("com.google.gwt:gwt-user:")) {
        Set<String> paths = node.getData().getTarget().getPaths(LibraryPathType.BINARY);
        final String path = ContainerUtil.getFirstItem(paths);
        if (path != null) {
          final Module module = modelsProvider.findIdeModule(node.getData().getOwnerModule());
          if (module != null) {
            setupGwtFacet(module, path, modelsProvider);
          }
        }
      }
    }
  }

  private static void setupGwtFacet(@NotNull Module module, @NotNull String pathToGwtJar, @NotNull IdeModifiableModelsProvider modelsProvider) {
    final ModifiableFacetModel facetModel = modelsProvider.getModifiableFacetModel(module);
    GwtFacet gwtFacet = facetModel.getFacetByType(GwtFacetType.ID);
    if (gwtFacet == null) {
      GwtFacetType facetType = GwtFacetType.getInstance();
      gwtFacet = facetType.createFacet(module, facetType.getDefaultFacetName(), facetType.createDefaultConfiguration(), null);
      facetModel.addFacet(gwtFacet, ExternalSystemApiUtil.toExternalSource(GradleConstants.SYSTEM_ID));
    }
    GwtFacetConfiguration configuration = gwtFacet.getConfiguration();
    String[] possibleUrls = {GwtGradleSdkPaths.getSdkUrl(pathToGwtJar, false), GwtGradleSdkPaths.getSdkUrl(pathToGwtJar, true)};
    for (String url : possibleUrls) {
      GwtSdkType type = GwtSdkManager.getInstance().detectSdkType(url);
      if (type != null) {
        configuration.setGwtSdkType(type.getId());
        configuration.setGwtSdkUrl(url);
        break;
      }
    }
    if (configuration.getGwtSdkUrl() == null) {
      Logger.getInstance(GwtDependencyDataService.class).info("Cannot detect GWT SDK location by " + pathToGwtJar);
    }
  }
}
