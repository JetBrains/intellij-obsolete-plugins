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
public class DMCompositeFacetType extends FacetType<DMCompositeFacet, DMCompositeFacetConfiguration> {
  public DMCompositeFacetType() {
    super(DMCompositeFacet.ID, "dmServerComposite", getDisplayName());
  }

  @Override
  public DMCompositeFacetConfiguration createDefaultConfiguration() {
    return new DMCompositeFacetConfiguration();
  }

  @Override
  public DMCompositeFacet createFacet(@NotNull Module module,
                                      String name,
                                      @NotNull DMCompositeFacetConfiguration config,
                                      @Nullable Facet underlyingFacet) {
    return new DMCompositeFacet(this, module, name, config, underlyingFacet);
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
    return DmServerBundle.message("DMCompositeFacetType.name");
  }
}
