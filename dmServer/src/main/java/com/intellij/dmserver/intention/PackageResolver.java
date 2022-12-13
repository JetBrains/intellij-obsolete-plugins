package com.intellij.dmserver.intention;

import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.dmserver.artifacts.ManifestManager;
import com.intellij.dmserver.artifacts.ManifestUpdater;
import com.intellij.dmserver.editor.AvailableBundlesProvider;
import com.intellij.dmserver.editor.ExportedUnit;
import com.intellij.dmserver.editor.ExportedUnitImpl;
import com.intellij.dmserver.editor.UnitsCollector;
import com.intellij.dmserver.facet.DMBundleFacet;
import com.intellij.dmserver.util.DmServerBundle;
import com.intellij.dmserver.util.ManifestUtils;
import com.intellij.facet.ProjectFacetManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.*;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.lang.manifest.psi.HeaderValuePart;
import org.jetbrains.lang.manifest.psi.ManifestFile;
import org.osgi.framework.Constants;
import org.osgi.framework.Version;

import java.util.ArrayList;
import java.util.List;

public class PackageResolver extends UnitResolver {

  @Override
  public String getHeaderName() {
    return Constants.IMPORT_PACKAGE;
  }

  @Nls
  @Override
  protected String getProblemMessage(String unitName) {
    return DmServerBundle.message("UnknownImportedPackageInspection.problem.message.unknown-package", unitName);
  }

  @Override
  protected UnitsCollector getUnitsCollector(AvailableBundlesProvider provider) {
    return provider.getPackagesCollector();
  }

  @Override
  protected void registerProblem(ProblemsHolder problemsHolder, HeaderValuePart headerValue, TextRange textRange, String unitName) {
    Module module = findPackageBundleModule(problemsHolder.getProject(), unitName);
    if (module != null) {
      ManifestManager.FileWrapper manifestWrapper = ManifestManager.getBundleInstance().findManifest(module);
      if (manifestWrapper != null) {
        problemsHolder.registerProblem(headerValue,
                                       textRange,
                                       DmServerBundle.message("PackageResolver.problem.message.package-not-exported", unitName),
                                       new ExportPackageQuickFix(unitName, manifestWrapper.getFile()));
        return;
      }
    }

    super.registerProblem(problemsHolder, headerValue, textRange, unitName);
  }

  private static Module findPackageBundleModule(Project project, String unitName) {
    PsiPackage psiPackage = JavaPsiFacade.getInstance(project).findPackage(unitName);
    if (psiPackage == null) {
      return null;
    }

    for (Module module : ProjectFacetManager.getInstance(project).getModulesWithFacet(DMBundleFacet.ID)) {
      PsiDirectory[] directories = psiPackage.getDirectories(module.getModuleScope(false));
      if (directories.length != 0) {
        return module;
      }
    }
    return null;
  }

  private static final class ExportPackageQuickFix implements LocalQuickFix {
    private final String myPackageName;
    private final SmartPsiElementPointer<ManifestFile> myManifest;

    private ExportPackageQuickFix(String packageName, ManifestFile manifest) {
      myPackageName = packageName;
      myManifest = SmartPointerManager.getInstance(manifest.getProject()).createSmartPsiElementPointer(manifest);
    }

    @Override
    @NotNull
    public String getName() {
      return DmServerBundle.message("PackageResolver.ExportPackageQuickFix.name");
    }

    @Override
    @NotNull
    public String getFamilyName() {
      return DmServerBundle.message("PackageResolver.ExportPackageQuickFix.family.name");
    }

    @Override
    public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
      ManifestFile manifest = myManifest.getElement();
      if (manifest == null) return;
      List<ExportedUnit> exportedPackages = new ArrayList<>(AvailableBundlesProvider.getExportedPackages(manifest));
      exportedPackages.add(new ExportedUnitImpl(myPackageName, Version.emptyVersion.toString()));

      ManifestUpdater manifestUpdater = new ManifestUpdater(manifest);
      StringBuilder headerValue = new StringBuilder();
      for (ExportedUnit exportedPackage : exportedPackages) {
        if (headerValue.length() > 0) {
          headerValue.append(",\n ");
        }
        headerValue.append(exportedPackage.getSymbolicName());

        if (!Version.emptyVersion.equals(exportedPackage.getVersion())) {
          headerValue.append(";");
          headerValue.append(ManifestUtils.VERSION_RANGE_ATTRIBUTE_NAME);
          headerValue.append("=\"");
          headerValue.append(exportedPackage.getVersion().toString());
          headerValue.append("\"");
        }
      }
      manifestUpdater.updateHeader(Constants.EXPORT_PACKAGE, headerValue.toString(), false);
    }
  }
}
