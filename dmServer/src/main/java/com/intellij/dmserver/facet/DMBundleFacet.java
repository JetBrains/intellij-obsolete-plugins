package com.intellij.dmserver.facet;

import com.intellij.dmserver.artifacts.DMBundleArtifactType;
import com.intellij.dmserver.artifacts.ManifestManager;
import com.intellij.facet.*;
import com.intellij.ide.fileTemplates.FileTemplate;
import com.intellij.ide.fileTemplates.FileTemplateManager;
import com.intellij.ide.util.frameworkSupport.FrameworkSupportProvider;
import com.intellij.javaee.web.facet.WebFacet;
import com.intellij.javaee.web.facet.WebFacetType;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.openapi.roots.ui.configuration.ModulesProvider;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.spring.facet.SpringFacet;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.osgi.jps.model.ManifestGenerationMode;
import org.osmorc.facet.OsmorcFacet;
import org.osmorc.facet.OsmorcFacetConfiguration;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class DMBundleFacet extends DMFacetBase<DMBundleFacetConfiguration> {

  private static final Logger LOG = Logger.getInstance(DMBundleFacet.class);

  @NonNls
  private static final String SPRING_DIR = "spring";

  @NonNls
  public static final String SPRING_PATH = ManifestManager.META_INF_DIR + "/" + SPRING_DIR;

  public static final FacetTypeId<DMBundleFacet> ID = new FacetTypeId<>("dmServerBundle");

  public DMBundleFacet(@NotNull DMBundleFacetType facetType,
                       @NotNull Module module,
                       @NotNull String name,
                       @NotNull DMBundleFacetConfiguration configuration,
                       @Nullable Facet underlyingFacet) {
    super(facetType, module, name, configuration, underlyingFacet);
  }

  @Nullable
  public static DMBundleFacet getInstance(Module module) {
    return FacetManager.getInstance(module).getFacetByType(ID);
  }

  public static boolean hasDmFacet(@NotNull PsiElement element) {
    return getInstance(element) != null;
  }

  @Nullable
  public static DMBundleFacet getInstance(@NotNull PsiElement element) {
    Module module = ModuleUtilCore.findModuleForPsiElement(element);
    if (module != null) {
      return getInstance(module);
    }
    return null;
  }

  @Override
  public DMBundleFacetConfiguration getConfigurationImpl() {
    return (DMBundleFacetConfiguration)getConfiguration();
  }

  @Override
  @NotNull
  public Collection<VirtualFile> getFacetRoots() {
    List<VirtualFile> result = new ArrayList<>();
    VirtualFile manifestFolder = ManifestManager.getBundleInstance().findManifestFolder(getModule());
    if (manifestFolder != null) {
      result.add(manifestFolder);
    }
    return result;
  }

  @NotNull
  @Override
  public DMBundleArtifactType selectMainArtifactType() {
    return DMBundleArtifactType.getInstance();
  }

  @Override
  public void updateSupport(final ModifiableRootModel rootModel, final ModulesProvider modulesProvider) {
    try {
      WriteAction.run(()-> doUpdateSupport(rootModel, modulesProvider));
    }
    catch (IOException e) {
      LOG.error(e);
    }
  }

  private void doUpdateSupport(ModifiableRootModel rootModel, ModulesProvider modulesProvider) throws IOException {

    Module module = getModule();
    DMBundleFacetConfiguration facetConfiguration = getConfigurationImpl();

    OsmorcFacet osmorcFacet = (OsmorcFacet)getUnderlyingFacet();
    osmorcFacet.getConfiguration().setManifestGenerationMode(ManifestGenerationMode.Manually);
    VirtualFile manifest = ManifestManager.getBundleInstance().createManifest(module, osmorcFacet, rootModel, null, null);

    JarContentUtil<VirtualFile> springJarContentUtil = new JarContentUtil<>(osmorcFacet) {
      @Override
      protected Pair<String, String> createJarContentsItem(VirtualFile metaInfDir) {
        return doCreateJarContentsItem(metaInfDir.getPath() + "/" + SPRING_DIR, SPRING_PATH);
      }
    };
    if (facetConfiguration.getIsSpringDM()) {
      addRequiredFacet(module, SpringFacet.getSpringFacetType(), modulesProvider);
      if (manifest != null) {
        VirtualFile metaInfDir = manifest.getParent();
        VirtualFile springDir = findOrCreateChildDirectory(metaInfDir, SPRING_DIR);
        createFileFromTemplate(DMTemplateGroupDescriptorFactory.DM_SPRING_MODULE_CONTEXT_TEMPLATE, springDir, "module-context.xml");
        createFileFromTemplate(DMTemplateGroupDescriptorFactory.DM_SPRING_OSGI_CONTEXT_TEMPLATE, springDir, "osgi-context.xml");
        springJarContentUtil.addItem(metaInfDir);
      }
    }
    else {
      removeRequiredFacet(module, SpringFacet.getSpringFacetType(), modulesProvider);
      if (manifest != null) {
        VirtualFile metaInfDir = manifest.getParent();
        springJarContentUtil.removeItem(metaInfDir);
      }
    }

    if (facetConfiguration.getIsWebModule()) {
      WebFacet newWebFacet = addRequiredFacet(module, WebFacetType.getInstance(), modulesProvider);
      if (newWebFacet != null) {
        DMServerSupportProvider supportProvider = FrameworkSupportProvider.EXTENSION_POINT.findExtension(DMServerSupportProvider.class);
        supportProvider.getBundleSupportProvider().getWebSupportProvider().addSupport(newWebFacet,
                                                                                      rootModel,
                                                                                      facetConfiguration.getWebFrameworkVersionName());
      }
    }
    else {
      removeRequiredFacet(module, WebFacetType.getInstance(), modulesProvider);
    }
  }

  private abstract static class RequiredFacetProcessor<F extends Facet, C extends FacetConfiguration> {

    public List<F> process(@NotNull Module module, @NotNull FacetType<F, C> type, @Nullable ModulesProvider modulesProvider) {
      FacetManager manager = FacetManager.getInstance(module);
      FacetModel model = modulesProvider == null ? manager : modulesProvider.getFacetModel(module);
      List<F> facets = new ArrayList<>(model.getFacetsByType(type.getId()));
      doProcessFacetsList(facets, manager, model, type);
      return facets;
    }

    protected void doProcessFacetsList(List<F> facets, FacetManager manager, FacetModel model, FacetType<F, C> type) {
      if (facets.isEmpty()) {
        return;
      }
      if (model instanceof ModifiableFacetModel) {
        for (F facet : facets) {
          doProcessFacet((ModifiableFacetModel)model, facet);
        }
      }
      else {
        ModifiableFacetModel modifiableModel = manager.createModifiableModel();
        for (F facet : facets) {
          doProcessFacet(modifiableModel, facet);
        }
        modifiableModel.commit();
      }
    }

    protected abstract void doProcessFacet(ModifiableFacetModel modifiableModel, F facet);
  }

  private VirtualFile findOrCreateChildDirectory(VirtualFile parent, final String name) throws IOException {
    VirtualFile child = parent.findChild(name);
    if (child != null) {
      return child;
    }
    return parent.createChildDirectory(this, name);
  }

  @Nullable
  private VirtualFile createFileFromTemplate(final String templateName, final VirtualFile parent, @NonNls final String fileName) {
    final FileTemplate template = FileTemplateManager.getDefaultInstance().getJ2eeTemplate(templateName);
    try {
      final String text = template.getText(FileTemplateManager.getDefaultInstance().getDefaultProperties());
      VirtualFile file = parent.findChild(fileName);
      if (file == null) {
        file = parent.createChildData(this, fileName);
        VfsUtil.saveText(file, text);
      }
      return file;
    }
    catch (IOException e) {
      LOG.error(e);
      return null;
    }
  }

  public static <F extends Facet, C extends FacetConfiguration> F addRequiredFacet(Module module,
                                                                                   @NotNull FacetType<F, C> facetType,
                                                                                   @Nullable ModulesProvider modulesProvider) {
    final Ref<F> result = new Ref<>(null);
    new RequiredFacetProcessor<F, C>() {

      @Override
      protected void doProcessFacetsList(List<F> facets, FacetManager manager, FacetModel model, FacetType<F, C> type) {
        if (!facets.isEmpty()) {
          return;
        }
        F newFacet = manager.createFacet(type, type.getDefaultFacetName(), null);
        facets.add(newFacet);
        result.set(newFacet);
        super.doProcessFacetsList(facets, manager, model, type);
      }

      @Override
      protected void doProcessFacet(ModifiableFacetModel modifiableModel, F facet) {
        modifiableModel.addFacet(facet);
      }
    }.process(module, facetType, modulesProvider);
    return result.get();
  }

  private static <F extends Facet, C extends FacetConfiguration> void removeRequiredFacet(Module module,
                                                                                          @NotNull FacetType<F, C> facetType,
                                                                                          @NotNull ModulesProvider modulesProvider) {
    new RequiredFacetProcessor<F, C>() {

      @Override
      protected void doProcessFacet(ModifiableFacetModel modifiableModel, F facet) {
        modifiableModel.removeFacet(facet);
      }
    }.process(module, facetType, modulesProvider);
  }

  private static abstract class JarContentUtil<T> {
    private final OsmorcFacetConfiguration myConfiguration;

    JarContentUtil(OsmorcFacet osmorcFacet) {
      myConfiguration = osmorcFacet.getConfiguration();
    }

    public void addItem(T item) {
      Pair<String, String> contentsItem = createJarContentsItem(item);
      List<Pair<String, String>> contents = myConfiguration.getAdditionalJARContents();
      if (!contents.contains(contentsItem)) {
        ArrayList<Pair<String, String>> newContents = new ArrayList<>(contents);
        newContents.add(contentsItem);
        myConfiguration.setAdditionalJARContents(newContents);
      }
    }

    public void removeItem(T item) {
      Pair<String, String> contentsItem = createJarContentsItem(item);
      List<Pair<String, String>> contents = myConfiguration.getAdditionalJARContents();
      if (contents.contains(contentsItem)) {
        ArrayList<Pair<String, String>> newContents = new ArrayList<>(contents);
        newContents.remove(contentsItem);
        myConfiguration.setAdditionalJARContents(newContents);
      }
    }

    protected static Pair<String, String> doCreateJarContentsItem(String sourcePath, String destinationPath) {
      return Pair.create(FileUtil.toSystemIndependentName(sourcePath), FileUtil.toSystemIndependentName(destinationPath));
    }

    protected abstract Pair<String, String> createJarContentsItem(T item);
  }
}
