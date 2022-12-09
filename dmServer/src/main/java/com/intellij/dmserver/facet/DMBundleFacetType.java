package com.intellij.dmserver.facet;

import com.intellij.dmserver.util.DmServerBundle;
import com.intellij.facet.Facet;
import com.intellij.facet.FacetType;
import com.intellij.facet.ui.FacetEditor;
import com.intellij.facet.ui.MultipleFacetSettingsEditor;
import com.intellij.openapi.module.JavaModuleType;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleType;
import com.intellij.openapi.project.Project;
import icons.DmServerSupportIcons;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.osmorc.facet.OsmorcFacetType;

import javax.swing.*;

public class DMBundleFacetType extends FacetType<DMBundleFacet, DMBundleFacetConfiguration> {
  public DMBundleFacetType() {
    super(DMBundleFacet.ID, "dmServerBundle", getDisplayName(), OsmorcFacetType.ID);
  }

  @Override
  public DMBundleFacetConfiguration createDefaultConfiguration() {
    return new DMBundleFacetConfiguration();
  }

  @Override
  public DMBundleFacet createFacet(@NotNull Module module,
                                   String name,
                                   @NotNull DMBundleFacetConfiguration config,
                                   @Nullable Facet underlyingFacet) {
    return new DMBundleFacet(this, module, name, config, underlyingFacet);
  }

  @Override
  public boolean isSuitableModuleType(ModuleType moduleType) {
    return moduleType instanceof JavaModuleType;
  }

  @NotNull
  @Override
  public String getDefaultFacetName() {
    return getDisplayName();
  }

  @Override
  public Icon getIcon() {
    return DmServerSupportIcons.DM;
  }

  @Override
  public MultipleFacetSettingsEditor createMultipleConfigurationsEditor(@NotNull Project project, FacetEditor @NotNull [] editors) {
    return new DMMultipleFacetSettingsEditor(project, editors);
  }

  @Nls
  public static String getDisplayName() {
    return DmServerBundle.message("DMBundleFacetType.display.name");
  }
}
