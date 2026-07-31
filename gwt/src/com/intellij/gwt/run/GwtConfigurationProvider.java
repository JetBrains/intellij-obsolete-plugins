package com.intellij.gwt.run;

import com.intellij.execution.Location;
import com.intellij.execution.RunManager;
import com.intellij.execution.actions.ConfigurationContext;
import com.intellij.execution.actions.LazyRunConfigurationProducer;
import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.gwt.facet.GwtFacet;
import com.intellij.gwt.module.GwtModulesManager;
import com.intellij.gwt.module.model.GwtModule;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.xml.XmlFile;
import com.intellij.util.PathUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class GwtConfigurationProvider extends LazyRunConfigurationProducer<GwtRunConfiguration> {
  @Override
  public @NotNull ConfigurationFactory getConfigurationFactory() {
    return GwtRunConfigurationType.getFactory();
  }

  @Override
  protected boolean setupConfigurationFromContext(@NotNull GwtRunConfiguration configuration,
                                                  @NotNull ConfigurationContext context,
                                                  @NotNull Ref<PsiElement> sourceElement) {
    GwtEntryPointData data = findGwtModule(context.getLocation());
    if (data != null) {
      RunManager runManager = RunManager.getInstance(context.getProject());
      GwtRunConfiguration gwtConfiguration = configuration;
      gwtConfiguration.setName(FileUtil.getNameWithoutExtension(PathUtil.getFileName(data.getHtmlPagePath())));
      gwtConfiguration.setModule(data.getGwtModule().getModule());
      gwtConfiguration.setPage(data.getHtmlPagePath());
      if (data.getFacet().getSdkVersion().isSuperDevModeUsedByDefault()) {
        gwtConfiguration.getGwtState().USE_SUPER_DEV_MODE = true;
      }
      return true;
    }
    return false;
  }


  @Override
  public boolean isConfigurationFromContext(@NotNull GwtRunConfiguration configuration, @NotNull ConfigurationContext context) {
    GwtEntryPointData data = findGwtModule(context.getLocation());
    if (data == null) return false;

    String pagePath1 = configuration.getPage();
    Module module1 = configuration.getModule();

    String pagePath2 = data.getHtmlPagePath();
    Module module2 = data.getGwtModule().getModule();
    return pagePath2.equals(pagePath1) && Comparing.equal(module1, module2);
  }

  private static @Nullable GwtEntryPointData findGwtModule(Location<?> location) {
    if (location == null) return null;

    PsiFile psiFile = location.getPsiElement().getContainingFile();
    if (psiFile == null) return null;

    VirtualFile file = psiFile.getVirtualFile();
    if (file == null) return null;

    GwtFacet facet = GwtFacet.findFacetBySourceFile(location.getProject(), file);
    if (facet == null) return null;

    GwtModulesManager gwtModulesManager = GwtModulesManager.getInstance(location.getProject());
    GwtModule gwtModule = gwtModulesManager.getGwtModuleByXmlFile(psiFile);
    if (gwtModule != null) {
      return getModuleWithFile(facet, gwtModulesManager, gwtModule);
    }

    if (psiFile instanceof PsiJavaFile) {
      PsiClass[] classes = ((PsiJavaFile)psiFile).getClasses();
      if (classes.length == 1) {
        PsiClass psiClass = classes[0];
        GwtModule module = gwtModulesManager.findGwtModuleByEntryPoint(psiClass);
        if (module != null) {
          return getModuleWithFile(facet, gwtModulesManager, module);
        }
      }
    }
    return null;
  }

  private static @Nullable GwtEntryPointData getModuleWithFile(@NotNull GwtFacet facet,
                                                               @NotNull GwtModulesManager gwtModulesManager,
                                                               @NotNull GwtModule gwtModule) {
    XmlFile[] psiHtmlFiles = gwtModulesManager.findHtmlFilesByModule(gwtModule);
    for (XmlFile psiHtmlFile : psiHtmlFiles) {
      VirtualFile htmlFile = psiHtmlFile.getVirtualFile();
      if (htmlFile != null) {
        String path = gwtModulesManager.getOutputPath(gwtModule, htmlFile);
        if (path != null) {
          return new GwtEntryPointData(facet, gwtModule, path);
        }
      }
    }
    return null;
  }

  private static final class GwtEntryPointData {
    private final GwtFacet myFacet;
    private final GwtModule myGwtModule;
    private final String myHtmlPagePath;

    private GwtEntryPointData(@NotNull GwtFacet facet, @NotNull GwtModule gwtModule, @NotNull String htmlPagePath) {
      myFacet = facet;
      myGwtModule = gwtModule;
      myHtmlPagePath = htmlPagePath;
    }

    private GwtFacet getFacet() {
      return myFacet;
    }

    private GwtModule getGwtModule() {
      return myGwtModule;
    }

    private String getHtmlPagePath() {
      return myHtmlPagePath;
    }
  }
}
