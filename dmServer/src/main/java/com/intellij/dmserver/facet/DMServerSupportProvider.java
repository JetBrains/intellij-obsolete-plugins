package com.intellij.dmserver.facet;

import com.intellij.dmserver.util.DmServerBundle;
import com.intellij.facet.FacetManager;
import com.intellij.facet.FacetTypeId;
import com.intellij.ide.util.frameworkSupport.FrameworkRole;
import com.intellij.ide.util.frameworkSupport.FrameworkSupportConfigurable;
import com.intellij.ide.util.frameworkSupport.FrameworkSupportModel;
import com.intellij.ide.util.frameworkSupport.FrameworkSupportProvider;
import com.intellij.javaee.framework.JavaeeProjectCategory;
import com.intellij.openapi.module.JavaModuleType;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleType;
import icons.DmServerSupportIcons;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class DMServerSupportProvider extends FrameworkSupportProvider {

  @NonNls
  private static final String ID = "framework:dmServer";

  private static final FacetTypeId<?>[] PROVIDED_FACET_IDS
    = new FacetTypeId<?>[]{DMBundleFacet.ID, DMCompositeFacet.ID, DMConfigFacet.ID};

  private final DMBundleSupportProvider myBundleSupportProvider;
  private final DMConfigSupportProvider myConfigSupportProvider;
  private final DMCompositeSupportProvider myCompositeSupportProvider;

  public DMServerSupportProvider() {
    super(ID, getText());
    myBundleSupportProvider = new DMBundleSupportProvider();
    myCompositeSupportProvider = new DMCompositeSupportProvider();
    myConfigSupportProvider = new DMConfigSupportProvider();
  }

  public DMBundleSupportProvider getBundleSupportProvider() {
    return myBundleSupportProvider;
  }

  public DMCompositeSupportProvider getCompositeSupportProvider() {
    return myCompositeSupportProvider;
  }

  public DMConfigSupportProvider getConfigSupportProvider() {
    return myConfigSupportProvider;
  }

  @Override
  @NotNull
  public FrameworkSupportConfigurable createConfigurable(@NotNull FrameworkSupportModel model) {
    return new DMServerSupportConfigurable(model, this);
  }

  @Override
  @Nullable
  public Icon getIcon() {
    return DmServerSupportIcons.DM;
  }

  @Override
  public boolean isEnabledForModuleType(@NotNull ModuleType moduleType) {
    return moduleType instanceof JavaModuleType;
  }


  @Override
  public boolean isSupportAlreadyAdded(@NotNull Module module) {
    FacetManager facetManager = FacetManager.getInstance(module);
    for (FacetTypeId<?> facetId : PROVIDED_FACET_IDS) {
      if (!facetManager.getFacetsByType(facetId).isEmpty()) {
        return true;
      }
    }
    return false;
  }

  @Override
  public FrameworkRole[] getRoles() {
    return new FrameworkRole[] {JavaeeProjectCategory.ROLE };
  }

  private static @Nls(capitalization = Nls.Capitalization.Title) String getText() {
    return DmServerBundle.message("DMServerSupportProvider.title");
  }
}
