package com.intellij.dmserver.facet;

import com.intellij.dmserver.install.DMServerInstallation;
import com.intellij.facet.FacetManager;
import com.intellij.facet.FacetTypeId;
import com.intellij.facet.FacetTypeRegistry;
import com.intellij.facet.ui.FacetBasedFrameworkSupportProvider;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.openapi.roots.ui.configuration.DefaultModulesProvider;
import org.jetbrains.annotations.NotNull;


public abstract class DMFacetSupportProviderBase<T extends DMFacetBase<C>, C extends DMFacetConfigurationBase<C>>
  extends FacetBasedFrameworkSupportProvider<T> {

  private static final Logger LOG = Logger.getInstance(DMBundleSupportProvider.class);

  private final FacetTypeId<T> myTypeId;

  public DMFacetSupportProviderBase(FacetTypeId<T> typeId) {
    super(FacetTypeRegistry.getInstance().findFacetType(typeId));
    myTypeId = typeId;
  }

  public void addDMSupport(@NotNull Module module,
                           @NotNull ModifiableRootModel rootModel) {
    super.addSupport(module, rootModel, null, null);
  }

  public void finishAddDMSupport(Module module, ModifiableRootModel rootModel, DMServerInstallation installation, C facetConfiguration) {
    T facet = FacetManager.getInstance(module).getFacetByType(myTypeId);
    LOG.assertTrue(facet != null);
    facet.getConfigurationImpl().loadState(facetConfiguration);

    doFinishAddDMSupport(module, rootModel, installation, facet);

    facet.updateSupportWithArtifact(rootModel, new DefaultModulesProvider(module.getProject()));
  }

  protected void doFinishAddDMSupport(Module module, ModifiableRootModel rootModel, DMServerInstallation installation, T facet) {

  }
}
