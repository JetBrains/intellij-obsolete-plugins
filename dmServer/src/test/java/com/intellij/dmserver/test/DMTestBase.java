package com.intellij.dmserver.test;

import com.intellij.dmserver.facet.*;
import com.intellij.facet.Facet;
import com.intellij.facet.FacetManager;
import com.intellij.facet.FacetType;
import com.intellij.facet.impl.ProjectFacetsConfigurator;
import com.intellij.facet.ui.FacetEditor;
import com.intellij.facet.ui.FacetEditorTab;
import com.intellij.ide.highlighter.ModuleFileType;
import com.intellij.ide.util.projectWizard.JavaModuleBuilder;
import com.intellij.javaee.web.facet.WebFacetType;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.roots.CompilerProjectExtension;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.roots.ModuleRootModel;
import com.intellij.openapi.roots.ui.configuration.ModulesConfigurator;
import com.intellij.openapi.roots.ui.configuration.projectRoot.StructureConfigurableContext;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.testFramework.JavaProjectTestCase;
import org.jetbrains.annotations.NonNls;
import org.osmorc.facet.OsmorcFacet;
import org.osmorc.facet.OsmorcFacetType;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public abstract class DMTestBase extends JavaProjectTestCase {
  private JavaModuleBuilder setupModuleBuilder(String moduleName) {
    JavaModuleBuilder moduleBuilder = new JavaModuleBuilder();
    moduleBuilder.setName(moduleName);
    moduleBuilder.setModuleFilePath(
      getProject().getBasePath() +
      File.separator +
      moduleName +
      File.separator +
      moduleName +
      ModuleFileType.DOT_DEFAULT_EXTENSION);
    return moduleBuilder;
  }

  protected final Module createJavaModule(String name) {
    Module result = setupModuleBuilder(name).commitModule(getProject(), null);
    refreshModuleRootDirectory(ModuleRootManager.getInstance(result));
    return result;
  }

  protected static OsmorcFacet createOsmorcFacet(Module module) {
    return createFacet(module, OsmorcFacetType.getInstance(), null);
  }

  protected static <T extends Facet<?>> T createFacet(final Module module, final FacetType<T, ?> facetType, final Facet parentFacet) {
    return WriteAction.compute(() -> {
      return FacetManager.getInstance(module).addFacet(facetType, facetType.getDefaultFacetName(), parentFacet);
    });
  }

  protected final Module initBundleModule(String name) throws Throwable {
    return doInitBundleModule(name, false);
  }

  protected final Module initWebBundleModule(String name) throws Throwable {
    return doInitBundleModule(name, true);
  }

  private Module doInitBundleModule(String name, final boolean isWeb) throws Throwable {
    final Module module = createJavaModule(name);

    final OsmorcFacet osmorcFacet = createOsmorcFacet(module);

    final DMBundleFacetType bundleFacetType = FacetType.findInstance(DMBundleFacetType.class);
    final DMBundleFacet bundleFacet = createFacet(module, bundleFacetType, osmorcFacet);

    if (isWeb) {
      new FacetEditorWrapper() {

        @Override
        protected void doProcessEditor(FacetEditor facetEditor) {

        }

        @Override
        protected Facet getFacet(Module module, ProjectFacetsConfigurator projectFacetsConfigurator) {
          return projectFacetsConfigurator.createAndAddFacet(module, WebFacetType.getInstance(), null);
        }
      }.processFacetEditor(module);
    }

    new FacetEditorWrapper() {

      @Override
      protected void doProcessEditor(FacetEditor facetEditor) {
        FacetEditorTab facetEditorTab = assertOneElement(facetEditor.getEditorTabs());
        assertInstanceOf(facetEditorTab, DMBundleFacetEditor.class);
      }

      @Override
      protected Facet getFacet(Module module, ProjectFacetsConfigurator projectFacetsConfigurator) {
        return bundleFacet;
      }
    }.processFacetEditor(module);

    return module;
  }


  protected final Module initPlanModule(@NonNls String name, @NonNls String planName, Module[] nestedModules) throws Throwable {
    return initCompositeModule(name, planName, DMCompositeType.PLAN, nestedModules);
  }

  protected final Module initParModule(@NonNls String name, @NonNls String planName, Module[] nestedModules) throws Throwable {
    return initCompositeModule(name, planName, DMCompositeType.PAR, nestedModules);
  }

  private Module initCompositeModule(@NonNls String name,
                                     @NonNls String symbolicName,
                                     DMCompositeType compositeType,
                                     Module[] nestedModules) throws Throwable {
    final Module module = createJavaModule(name);

    final DMCompositeFacetType compositeFacetType = FacetType.findInstance(DMCompositeFacetType.class);

    final DMCompositeFacet compositeFacet = createFacet(module, compositeFacetType, null);

    DMCompositeFacetConfiguration compositeFacetConfiguration = compositeFacet.getConfigurationImpl();
    compositeFacetConfiguration.setName(symbolicName);
    compositeFacetConfiguration.setCompositeType(compositeType);
    List<NestedUnitIdentity> nestedUnits = new ArrayList<>();
    for (Module nestedModule : nestedModules) {
      nestedUnits.add(new NestedUnitIdentity(nestedModule));
    }
    compositeFacetConfiguration.setNestedBundles(nestedUnits);

    new FacetEditorWrapper() {

      @Override
      protected void doProcessEditor(FacetEditor facetEditor) {
        FacetEditorTab facetEditorTab = assertOneElement(facetEditor.getEditorTabs());
        assertInstanceOf(facetEditorTab, DMCompositeFacetEditor.class);
      }

      @Override
      protected Facet getFacet(Module module, ProjectFacetsConfigurator projectFacetsConfigurator) {
        return compositeFacet;
      }
    }.processFacetEditor(module);

    return module;
  }

  protected static void refreshModuleRootDirectory(final ModuleRootModel rootModel) {
    refreshDirectory(getContentRoot(rootModel));
  }

  protected static void refreshDirectory(final VirtualFile dir) {
    WriteAction.runAndWait(() -> {
      dir.getChildren();
      dir.refresh(false, true);
    });
  }

  protected static VirtualFile getContentRoot(ModuleRootModel rootModel) {
    return rootModel.getContentRoots()[0];
  }

  protected static VirtualFile getContentRoot(Module module) {
    return getContentRoot(ModuleRootManager.getInstance(module));
  }

  protected final void setupProjectOutput() {
    String projectOutputPath = FileUtil.toSystemIndependentName(getProject().getBasePath() + "/out");
    CompilerProjectExtension.getInstance(getProject()).setCompilerOutputUrl(VfsUtilCore.pathToUrl(projectOutputPath));
  }

  private abstract class FacetEditorWrapper {
    public void processFacetEditor(Module module) throws ConfigurationException {
      ModulesConfigurator modulesConfigurator = new ModulesConfigurator(getProject());

      StructureConfigurableContext context = new StructureConfigurableContext(getProject(), modulesConfigurator);

      modulesConfigurator.setContext(context);

      modulesConfigurator.getOrCreateModuleEditor(module);

      ProjectFacetsConfigurator projectFacetsConfigurator = modulesConfigurator.getFacetsConfigurator();

      Facet facet = getFacet(module, projectFacetsConfigurator);

      FacetEditor facetEditor = projectFacetsConfigurator.getOrCreateEditor(facet);

      doProcessEditor(facetEditor);

      modulesConfigurator.apply();

      modulesConfigurator.disposeUIResources();
    }

    protected abstract Facet getFacet(Module module, ProjectFacetsConfigurator projectFacetsConfigurator);

    protected abstract void doProcessEditor(FacetEditor facetEditor);
  }
}
