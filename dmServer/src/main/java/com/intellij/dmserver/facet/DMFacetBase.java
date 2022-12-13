package com.intellij.dmserver.facet;

import com.intellij.dmserver.artifacts.DMArtifactTypeBase;
import com.intellij.dmserver.artifacts.WithModuleArtifactUtil;
import com.intellij.facet.Facet;
import com.intellij.facet.FacetType;
import com.intellij.javaee.facet.JavaeeFacet;
import com.intellij.javaee.facet.JavaeeFacetCommonPart;
import com.intellij.javaee.facet.JavaeeFacetEx;
import com.intellij.javaee.facet.JavaeeFacetListener;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.openapi.roots.ui.configuration.ModulesProvider;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.ModificationTracker;
import com.intellij.packaging.artifacts.Artifact;
import com.intellij.packaging.artifacts.ArtifactManager;
import com.intellij.packaging.artifacts.ModifiableArtifact;
import com.intellij.packaging.artifacts.ModifiableArtifactModel;
import com.intellij.util.descriptors.ConfigFileContainer;
import com.intellij.util.descriptors.ConfigFileInfoSet;
import com.intellij.util.descriptors.ConfigFileMetaDataProvider;
import com.intellij.util.descriptors.impl.ConfigFileInfoSetImpl;
import com.intellij.util.descriptors.impl.ConfigFileMetaDataRegistryImpl;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class DMFacetBase<C extends DMFacetConfigurationBase<C>> extends JavaeeFacet implements JavaeeFacetEx {

  private final JavaeeFacetCommonPart myCommonPart;

  public DMFacetBase(@NotNull FacetType dmFacetType,
                     @NotNull Module module,
                     @NotNull String name,
                     @NotNull C configuration,
                     @Nullable Facet underlyingFacet) {
    super(dmFacetType, module, name, configuration, underlyingFacet);

    ConfigFileMetaDataProvider metaDataProvider = new ConfigFileMetaDataRegistryImpl();
    ConfigFileInfoSet descriptorsConfig = new ConfigFileInfoSetImpl(metaDataProvider);
    myCommonPart = new JavaeeFacetCommonPart(this, metaDataProvider, descriptorsConfig);
    Disposer.register(this, myCommonPart);
  }

  @Override
  public JavaeeFacetCommonPart getCommonPart() {
    return myCommonPart;
  }

  @Override
  public ConfigFileContainer getDescriptorsContainer() {
    return myCommonPart.getDescriptorsContainer();
  }

  @Override
  public ModificationTracker getModificationTracker() {
    return myCommonPart.getModificationTracker();
  }

  @Override
  public void addFacetListener(JavaeeFacetListener listener) {
    myCommonPart.addListener(listener);
  }

  @Override
  public void removeFacetListener(JavaeeFacetListener listener) {
    myCommonPart.removeListener(listener);
  }

  @Override
  public void onFacetChanged() {
    updateMainArtifact();
  }

  public void updateSupportWithArtifact(ModifiableRootModel rootModel, ModulesProvider modulesProvider) {
    updateSupport(rootModel, modulesProvider);
    updateMainArtifact();
  }

  public void updateMainArtifact() {
    WriteAction.run(() -> {
      DMArtifactTypeBase artifactType = selectMainArtifactType();
      Artifact mainArtifact = getMainArtifact();
      if (mainArtifact != null && artifactType.isCompatibleArtifact(mainArtifact)) {
        ModifiableArtifactModel modifiableModel = ArtifactManager.getInstance(getModule().getProject()).createModifiableModel();
        ModifiableArtifact modifiableArtifact = modifiableModel.getOrCreateModifiableArtifact(mainArtifact);
        artifactType.synchronizeArtifact(modifiableArtifact, getModule(), DMFacetBase.this);
        modifiableModel.commit();
        return;
      }
      if (mainArtifact != null) {
        ArtifactManager manager = ArtifactManager.getInstance(getModule().getProject());
        ModifiableArtifactModel model = manager.createModifiableModel();
        model.removeArtifact(mainArtifact);
        model.commit();
      }
      artifactType.createArtifactFor(getModule(), DMFacetBase.this);
    });
  }

  public Artifact getMainArtifact() {
    return WithModuleArtifactUtil.findDmBundleArtifactFor(getModule());
  }

  public abstract DMArtifactTypeBase selectMainArtifactType();

  public abstract C getConfigurationImpl();

  protected abstract void updateSupport(ModifiableRootModel rootModel, ModulesProvider modulesProvider);
}
