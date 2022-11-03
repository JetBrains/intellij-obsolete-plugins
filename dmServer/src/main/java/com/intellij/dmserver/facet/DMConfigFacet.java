package com.intellij.dmserver.facet;

import com.intellij.dmserver.artifacts.ConfigFileManager;
import com.intellij.dmserver.artifacts.DMConfigArtifactType;
import com.intellij.facet.Facet;
import com.intellij.facet.FacetManager;
import com.intellij.facet.FacetTypeId;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.openapi.roots.ui.configuration.ModulesProvider;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;

public class DMConfigFacet extends DMFacetBase<DMConfigFacetConfiguration> {
  public static final FacetTypeId<DMConfigFacet> ID = new FacetTypeId<>("dmServerConfig");

  private final ConfigFileManager myConfigFileManager;

  public DMConfigFacet(@NotNull DMConfigFacetType facetType,
                       @NotNull Module module,
                       @NotNull String name,
                       @NotNull DMConfigFacetConfiguration configuration,
                       @Nullable Facet underlyingFacet /* may not be needed */) {
    super(facetType, module, name, configuration, underlyingFacet);
    myConfigFileManager = new ConfigFileManager(module);
  }

  @Nullable
  public static DMConfigFacet getInstance(Module module) {
    return FacetManager.getInstance(module).getFacetByType(ID);
  }

  @Override
  public DMConfigFacetConfiguration getConfigurationImpl() {
    return (DMConfigFacetConfiguration)getConfiguration();
  }

  @Override
  @NotNull
  public Collection<VirtualFile> getFacetRoots() {
    return Collections.emptyList(); // TODO: implement with a sense
  }

  @NotNull
  @Override
  public DMConfigArtifactType selectMainArtifactType() {
    return DMConfigArtifactType.getInstance();
  }

  public ConfigFileManager getConfigFileManager() {
    return myConfigFileManager;
  }

  @Override
  public void updateSupport(ModifiableRootModel rootModel, ModulesProvider modulesProvider) {
    getConfigFileManager().createConfigFile(rootModel);
  }
}
