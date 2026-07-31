package com.intellij.gwt.references;

import com.intellij.gwt.facet.GwtFacet;
import com.intellij.gwt.module.GwtModulesManager;
import com.intellij.gwt.module.model.GwtModule;
import com.intellij.javaee.web.WebDirectoryElement;
import com.intellij.javaee.web.WebUtil;
import com.intellij.openapi.deployment.DeploymentUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementResolveResult;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiPolyVariantReferenceBase;
import com.intellij.psi.PsiReference;
import com.intellij.psi.PsiReferenceProvider;
import com.intellij.psi.ResolveResult;
import com.intellij.psi.xml.XmlAttributeValue;
import com.intellij.util.PathUtil;
import com.intellij.util.ProcessingContext;
import com.intellij.util.SmartList;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;

class GeneratedJsReferenceProvider extends PsiReferenceProvider {
  @Override
  public PsiReference @NotNull [] getReferencesByElement(@NotNull PsiElement element, @NotNull ProcessingContext context) {
    final PsiFile containingFile = element.getContainingFile();
    if (containingFile != null) {
      final VirtualFile file = containingFile.getVirtualFile();
      final Project project = element.getProject();
      final GwtFacet gwtFacet = GwtFacet.findFacetBySourceFile(project, file);
      if (gwtFacet != null) {
        if (element instanceof XmlAttributeValue attributeValue) {
          final String value = attributeValue.getValue();
          if (value.endsWith(GwtModuleInHtmlFileReferenceBase.NO_CACHE_JS_SUFFIX)) {
            final List<Pair<GwtModule,String>> gwtModulesByPublicFile = GwtModulesManager.getInstance(project).findGwtModulesByPublicFile(file);
            if (!gwtModulesByPublicFile.isEmpty()) {
              return new PsiReference[] {new GwtModuleInHtmlFileUnderPublicRootReference(file, attributeValue)};
            }
            else if (gwtFacet.getSdkVersion().isHtmlFilesOutsideSourcesAreAllowed()) {
              final WebDirectoryElement webDirectory = WebUtil.findWebDirectoryByFile(file, project);
              if (webDirectory != null) {
                final WebDirectoryElement parent = webDirectory.getParent();
                if (parent != null) {
                  return new PsiReference[] {new GwtModuleInHtmlFileUnderWebRootReference(parent.getPath(), attributeValue)};
                }
              }
            }
          }
        }
      }
    }
    return PsiReference.EMPTY_ARRAY;
  }

  private static class GwtModuleInHtmlFileUnderWebRootReference extends GwtModuleInHtmlFileReferenceBase {
    private final String myPathFromWebRoot;

    GwtModuleInHtmlFileUnderWebRootReference(String pathFromWebRoot, XmlAttributeValue attributeValue) {
      super(attributeValue);
      myPathFromWebRoot = pathFromWebRoot;
    }

    @Override
    protected void collectGwtModuleByPath(String path, List<GwtModule> modules) {
      String outputName = PathUtil.getFileName(path);
      final Collection<GwtModule> gwtModules = myModulesManager.findGwtModulesByOutputName(outputName, getElement().getContainingFile().getResolveScope());
      if (gwtModules.isEmpty()) return;

      final String canonicalPath = FileUtil.toCanonicalPath(DeploymentUtil.appendToPath(myPathFromWebRoot, path));

      if (FileUtil.pathsEqual(canonicalPath, "/" + outputName + "/" + outputName)) {
        modules.addAll(gwtModules);
      }
    }
  }

  private static final class GwtModuleInHtmlFileUnderPublicRootReference extends GwtModuleInHtmlFileReferenceBase {
    private final VirtualFile myFile;

    private GwtModuleInHtmlFileUnderPublicRootReference(VirtualFile file, XmlAttributeValue attributeValue) {
      super(attributeValue);
      myFile = file;
    }

    @Override
    protected void collectGwtModuleByPath(String path, List<GwtModule> result) {
      final List<Pair<GwtModule, String>> publicFile = myModulesManager.findGwtModulesByPublicFile(myFile);
      for (Pair<GwtModule, String> pair : publicFile) {
        final GwtModule module = pair.getFirst();
        if (PathUtil.getParentPath(pair.getSecond()).isEmpty() && path.equals(module.getOutputName())) {
          result.add(module);
        }
      }
    }
  }

  private abstract static class GwtModuleInHtmlFileReferenceBase extends PsiPolyVariantReferenceBase<XmlAttributeValue> {
    private static final @NonNls String NO_CACHE_JS_SUFFIX = ".nocache.js";
    protected GwtModulesManager myModulesManager;

    GwtModuleInHtmlFileReferenceBase(XmlAttributeValue attributeValue) {
      super(attributeValue, true);
      myModulesManager = GwtModulesManager.getInstance(attributeValue.getProject());
    }

    @Override
    public ResolveResult @NotNull [] multiResolve(boolean incompleteCode) {
      String path = StringUtil.trimEnd(getValue(), NO_CACHE_JS_SUFFIX);
      List<GwtModule> gwtModules = new SmartList<>();
      collectGwtModuleByPath(path, gwtModules);

      final ResolveResult[] resolveResults = new ResolveResult[gwtModules.size()];
      for (int i = 0; i < gwtModules.size(); i++) {
        resolveResults[i] = new PsiElementResolveResult(gwtModules.get(i).getModuleXmlFile());
      }
      return resolveResults;
    }

    protected abstract void collectGwtModuleByPath(String path, List<GwtModule> modules);
  }
}
