package com.intellij.dmserver.facet;

import com.intellij.dmserver.artifacts.*;
import com.intellij.dmserver.artifacts.plan.PlanFileManager;
import com.intellij.dmserver.util.PsiTreeChangedAdapter;
import com.intellij.facet.Facet;
import com.intellij.facet.FacetManager;
import com.intellij.facet.FacetTypeId;
import com.intellij.javaee.facet.JavaeeFacet;
import com.intellij.javaee.facet.JavaeeFacetListener;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.roots.ModuleRootModel;
import com.intellij.openapi.roots.ui.configuration.ModulesProvider;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.packaging.artifacts.ArtifactType;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiTreeChangeEvent;
import com.intellij.psi.PsiTreeChangeListener;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class DMCompositeFacet extends DMFacetBase<DMCompositeFacetConfiguration> {

  private static final Logger LOG = Logger.getInstance(DMCompositeFacet.class);

  public static final FacetTypeId<DMCompositeFacet> ID = new FacetTypeId<>("dmServerComposite");

  private static final Map<DMCompositeType, DMCompositeTypeSupport> ourSupportedArtifacts;

  static {
    Map<DMCompositeType, DMCompositeTypeSupport> supportedArtifacts = new HashMap<>();
    supportedArtifacts.put(DMCompositeType.PLAN, new DMPlanTypeSupport());
    supportedArtifacts.put(DMCompositeType.PAR, new DMParTypeSupport());
    ourSupportedArtifacts = Collections.unmodifiableMap(supportedArtifacts);
  }

  public static List<? extends ArtifactType> getSupportedArtifactTypes() {
    ArrayList<ArtifactType> result = new ArrayList<>();
    for (DMCompositeTypeSupport typeSupport : ourSupportedArtifacts.values()) {
      result.add(typeSupport.getArtifactType());
    }
    return result;
  }

  private final PlanFileManager myPlanFileManager;

  private final Set<DMFacetBase> myListenedNestedUnitFacets;

  private final JavaeeFacetListener myNestedUnitFacetListener;

  public DMCompositeFacet(@NotNull DMCompositeFacetType facetType,
                          @NotNull Module module,
                          @NotNull String name,
                          @NotNull DMCompositeFacetConfiguration configuration,
                          @Nullable Facet underlyingFacet /* may not be needed */) {
    super(facetType, module, name, configuration, underlyingFacet);
    myPlanFileManager = new PlanFileManager(module);

    myListenedNestedUnitFacets = new HashSet<>();
    myNestedUnitFacetListener = new JavaeeFacetListener() {

      @Override
      public void facetChanged(JavaeeFacet facet) {
        onNestedUnitFacetChanged(facet);
      }
    };
  }

  private void onNestedUnitFacetChanged(JavaeeFacet facet) {
    if (isDisposed()) {
      return;
    }
    if (!(LOG.assertTrue(facet instanceof DMFacetBase))) {
      return;
    }

    ApplicationManager.getApplication().invokeLater(() -> updateSupportWithArtifact());
  }

  @Override
  public void initFacet() {
    Project project = getModule().getProject();

    getConfigurationImpl().init(project);

    PsiManager psiManager = PsiManager.getInstance(project);
    PsiTreeChangeListener psiTreeChangeListener = new PsiTreeChangedAdapter() {

      @Override
      protected void treeChanged(PsiTreeChangeEvent event) {
        getTypeSupport().getConfigManager(DMCompositeFacet.this).onPsiEvent(event, DMCompositeFacet.this);
      }
    };
    psiManager.addPsiTreeChangeListener(psiTreeChangeListener, this);

    updateFacetListeners();

    new DMNestedBundlesUpdater(project, this) {

      @Override
      protected Collection<NestedUnitIdentity> getNestedBundles() {
        return getConfigurationImpl().getNestedBundles();
      }

      @Override
      protected void setNestedBundles(Collection<NestedUnitIdentity> nestedBundles) {
        getConfigurationImpl().setNestedBundles(nestedBundles);
      }

      @Override
      protected void dmFacetAddedOrRemoved(DMFacetBase facet) {
        Module module = facet.getModule();
        for (NestedUnitIdentity unitIdentity : getConfigurationImpl().getNestedBundles()) {
          Module nestedModule = unitIdentity.getModule();
          if (nestedModule == module || nestedModule == null) {
            updateSupportWithArtifact();
            break;
          }
        }
      }
    };
  }

  public void updateFacetListeners() {
    final Set<DMFacetBase> newListenedNestedUnitFacets = new HashSet<>();

    for (NestedUnitIdentity unitIdentity : getConfigurationImpl().getNestedBundles()) {
      Module nestedModule = unitIdentity.getModule();
      if (nestedModule == null) {
        continue;
      }

      new DMFacetsSwitch<>() {

        @Override
        protected Object doProcessBundleFacet(DMBundleFacet bundleFacet) {
          return new Object();
        }

        @Override
        protected Object doProcessCompositeFacet(DMCompositeFacet compositeFacet) {
          newListenedNestedUnitFacets.add(compositeFacet);
          return new Object();
        }

        @Override
        protected Object doProcessConfigFacet(DMConfigFacet configFacet) {
          return new Object();
        }
      }.processModule(nestedModule);
    }

    for (DMFacetBase listenedFacet : myListenedNestedUnitFacets.toArray(new DMFacetBase[0])) {
      if (newListenedNestedUnitFacets.contains(listenedFacet)) {
        newListenedNestedUnitFacets.remove(listenedFacet);
      }
      else {
        listenedFacet.removeFacetListener(myNestedUnitFacetListener);
        myListenedNestedUnitFacets.remove(listenedFacet);
      }
    }

    for (DMFacetBase newListenedFacet : newListenedNestedUnitFacets) {
      newListenedFacet.addFacetListener(myNestedUnitFacetListener);
      myListenedNestedUnitFacets.add(newListenedFacet);
    }
  }

  private void updateSupportWithArtifact() {
    updateSupportWithArtifact(null, null);
  }

  @Override
  protected void updateSupport(ModifiableRootModel modifiableRootModel, ModulesProvider modulesProvider) {
    Module module = getModule();
    ModuleRootModel rootModel = modifiableRootModel == null ? ModuleRootManager.getInstance(module) : modifiableRootModel;
    updateFacetListeners();
    selectMainArtifactType().updateModuleSupport(module, this, rootModel, getConfigurationImpl());
  }

  public PlanFileManager getPlanFileManager() {
    return myPlanFileManager;
  }

  @Nullable
  public static DMCompositeFacet getInstance(Module module) {
    return FacetManager.getInstance(module).getFacetByType(ID);
  }

  @Override
  public DMCompositeFacetConfiguration getConfigurationImpl() {
    return (DMCompositeFacetConfiguration)getConfiguration();
  }

  @Override
  @NotNull
  public Collection<VirtualFile> getFacetRoots() {
    List<VirtualFile> result = new ArrayList<>();
    VirtualFile manifestFolder = ManifestManager.getParInstance().findManifestFolder(getModule());
    if (manifestFolder != null) {
      result.add(manifestFolder);
    }
    return result;
  }

  @NotNull
  @Override
  public DMCompositeArtifactTypeBase selectMainArtifactType() {
    return getTypeSupport().getArtifactType();
  }

  private DMCompositeTypeSupport getTypeSupport() {
    return ourSupportedArtifacts.get(getConfigurationImpl().getCompositeType());
  }

  private interface DMCompositeTypeSupport {

    DMCompositeArtifactTypeBase getArtifactType();

    PsiConfigManagerBase<?, DMCompositeFacetConfiguration, DMCompositeFacet> getConfigManager(DMCompositeFacet facetInstance);
  }

  private static class DMPlanTypeSupport implements DMCompositeTypeSupport {

    @Override
    public DMCompositeArtifactTypeBase getArtifactType() {
      return DMPlanArtifactType.getInstance();
    }

    @Override
    public PsiConfigManagerBase<?, DMCompositeFacetConfiguration, DMCompositeFacet> getConfigManager(DMCompositeFacet facetInstance) {
      return facetInstance.myPlanFileManager;
    }
  }

  private static class DMParTypeSupport implements DMCompositeTypeSupport {

    @Override
    public DMCompositeArtifactTypeBase getArtifactType() {
      return DMParArtifactType.getInstance();
    }

    @Override
    public PsiConfigManagerBase<?, DMCompositeFacetConfiguration, DMCompositeFacet> getConfigManager(DMCompositeFacet facetInstance) {
      return ManifestManager.getParInstance();
    }
  }
}
