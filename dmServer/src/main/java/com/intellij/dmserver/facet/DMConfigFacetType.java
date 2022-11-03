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

import javax.swing.*;

// may have common super with DMBundleFacetType
public class DMConfigFacetType extends FacetType<DMConfigFacet, DMConfigFacetConfiguration> {
  public DMConfigFacetType() {
    super(DMConfigFacet.ID, "dmServerConfig", getDisplayName());
  }

  @Override
  public DMConfigFacetConfiguration createDefaultConfiguration() {
    return new DMConfigFacetConfiguration();
  }

  @Override
  public DMConfigFacet createFacet(@NotNull Module module,
                                   String name,
                                   @NotNull DMConfigFacetConfiguration config,
                                   @Nullable Facet underlyingFacet) {
    return new DMConfigFacet(this, module, name, config, underlyingFacet);
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
    return DmServerBundle.message("DMConfigFacetType.display.name");
  }
}
