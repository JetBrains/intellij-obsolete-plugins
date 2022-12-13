package com.intellij.dmserver.test;

import com.intellij.codeInspection.*;
import com.intellij.dmserver.artifacts.ManifestManager;
import com.intellij.dmserver.artifacts.ManifestUpdater;
import com.intellij.dmserver.editor.AvailableBundlesProvider;
import com.intellij.dmserver.install.DMServerInstallation;
import com.intellij.dmserver.integration.DMServerRepositoryExternalItem;
import com.intellij.dmserver.integration.DMServerRepositoryItem;
import com.intellij.dmserver.integration.DMServerRepositoryWatchedItem;
import com.intellij.dmserver.integration.RepositoryPattern;
import com.intellij.dmserver.intention.UnknownImportedPackageInspection;
import com.intellij.dmserver.util.DmServerBundle;
import com.intellij.dmserver.util.ManifestUtils;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElementVisitor;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.lang.manifest.psi.HeaderValuePart;
import org.jetbrains.lang.manifest.psi.ManifestFile;
import org.osgi.framework.Constants;
import org.osmorc.manifest.lang.psi.Clause;

import java.io.IOException;

public class DMIntentionTest extends DMFrameworkTestBase {
  public void testNotExistingPackageInspected() throws Throwable {
    @NonNls final String MODULE_NAME = "NotExistingPackageInspected";
    @NonNls final String NOT_EXISTING_PACKAGE_NAME = "not.existing.package";
    @NonNls final String VERSIONED_MODULE_NAME = "NotExistingVersionedPackageInspected";
    @NonNls final String NOT_EXISTING_VERSIONED_PACKAGE_NAME = "not.existing.versioned.package";

    createAndSetActiveInstallation();

    resetProvider();

    doTestNotExistingUnitInspected(MODULE_NAME, Constants.IMPORT_PACKAGE,
                                   NOT_EXISTING_PACKAGE_NAME,
                                   NOT_EXISTING_PACKAGE_NAME,
                                   DmServerBundle.message("UnknownImportedPackageInspection.problem.message.unknown-package",
                                                          NOT_EXISTING_PACKAGE_NAME));
    doTestNotExistingUnitInspected(VERSIONED_MODULE_NAME, Constants.IMPORT_PACKAGE,
                                   NOT_EXISTING_VERSIONED_PACKAGE_NAME + ";version=[1.0.0,2.0.0)",
                                   NOT_EXISTING_VERSIONED_PACKAGE_NAME,
                                   DmServerBundle.message("UnknownImportedPackageInspection.problem.message.unknown-package",
                                                          NOT_EXISTING_VERSIONED_PACKAGE_NAME));
  }

  public void testNotExistingBundleInspected() throws Throwable {
    @NonNls final String MODULE_NAME = "NotExistingBundleInspected";
    @NonNls final String NOT_EXISTING_BUNDLE_NAME = "not.existing.bundle";
    @NonNls final String VERSIONED_MODULE_NAME = "NotExistingVersionedBundleInspected";
    @NonNls final String NOT_EXISTING_VERSIONED_BUNDLE_NAME = "not.existing.versioned.bundle";

    createAndSetActiveInstallation();

    resetProvider();

    doTestNotExistingUnitInspected(MODULE_NAME, ManifestUtils.IMPORT_BUNDLE_HEADER,
                                   NOT_EXISTING_BUNDLE_NAME,
                                   NOT_EXISTING_BUNDLE_NAME,
                                   DmServerBundle.message("UnknownImportedPackageInspection.problem.message.unknown-bundle",
                                                          NOT_EXISTING_BUNDLE_NAME));
    doTestNotExistingUnitInspected(VERSIONED_MODULE_NAME, ManifestUtils.IMPORT_BUNDLE_HEADER,
                                   NOT_EXISTING_VERSIONED_BUNDLE_NAME + ";version=[1.0.0,2.0.0)",
                                   NOT_EXISTING_VERSIONED_BUNDLE_NAME,
                                   DmServerBundle.message("UnknownImportedPackageInspection.problem.message.unknown-bundle",
                                                          NOT_EXISTING_VERSIONED_BUNDLE_NAME));
  }

  public void testNotExistingLibraryInspected() throws Throwable {
    @NonNls final String MODULE_NAME = "NotExistingLibraryInspected";
    @NonNls final String NOT_EXISTING_LIBRARY_NAME = "not.existing.library";
    @NonNls final String VERSIONED_MODULE_NAME = "NotExistingVersionedLibraryInspected";
    @NonNls final String NOT_EXISTING_VERSIONED_LIBRARY_NAME = "not.existing.versioned.library";

    createAndSetActiveInstallation();

    resetProvider();

    doTestNotExistingUnitInspected(MODULE_NAME, ManifestUtils.IMPORT_LIBRARY_HEADER,
                                   NOT_EXISTING_LIBRARY_NAME,
                                   NOT_EXISTING_LIBRARY_NAME,
                                   DmServerBundle.message("UnknownImportedPackageInspection.problem.message.unknown-library",
                                                          NOT_EXISTING_LIBRARY_NAME));
    doTestNotExistingUnitInspected(VERSIONED_MODULE_NAME, ManifestUtils.IMPORT_LIBRARY_HEADER,
                                   NOT_EXISTING_VERSIONED_LIBRARY_NAME + ";version=[1.0.0,2.0.0)",
                                   NOT_EXISTING_VERSIONED_LIBRARY_NAME,
                                   DmServerBundle.message("UnknownImportedPackageInspection.problem.message.unknown-library",
                                                          NOT_EXISTING_VERSIONED_LIBRARY_NAME));
  }

  private void doTestNotExistingUnitInspected(String moduleName,
                                              String headerName,
                                              String headerValueText,
                                              String notExistingUnitName,
                                              String message)
    throws Throwable {

    Module bundleModule = initBundleModule(moduleName);
    HeaderValuePart headerValue = setBundleHeader(bundleModule, headerName, headerValueText);

    ProblemsHolder problemsHolder = inspect(headerValue);

    ProblemDescriptor problem = doTestProblem(problemsHolder, headerValue, notExistingUnitName, message);

    QuickFix fix = assertOneElement(problem.getFixes());
    assertEquals(DmServerBundle.message("UnknownImportedPackageInspection.DownloadQuickFix.name"), fix.getName());
  }

  private static ProblemDescriptor doTestProblem(ProblemsHolder problemsHolder,
                                                 HeaderValuePart headerValue,
                                                 String unitName,
                                                 String message) {
    ProblemDescriptor problem = assertOneElement(problemsHolder.getResults());
    assertEquals(headerValue, problem.getStartElement());
    assertEquals(headerValue, problem.getEndElement());
    assertEquals(message, problem.getDescriptionTemplate());
    assertInstanceOf(problem, ProblemDescriptorBase.class);
    TextRange textRange = ((ProblemDescriptorBase)problem).getTextRange();
    assertNotNull(textRange);
    assertEquals(unitName, textRange.substring(headerValue.getContainingFile().getText()));
    return problem;
  }

  private void resetProvider() {
    AvailableBundlesProvider.getInstance(getProject()).resetAll();
  }

  private ProblemsHolder inspect(HeaderValuePart headerValue) {
    UnknownImportedPackageInspection inspection = new UnknownImportedPackageInspection();
    ProblemsHolder problemsHolder = new ProblemsHolder(InspectionManager.getInstance(getProject()), headerValue.getContainingFile(), false);
    PsiElementVisitor visitor = inspection.buildVisitor(problemsHolder, false);
    visitor.visitElement(headerValue);
    return problemsHolder;
  }

  public void testExistingProjectBundleNotInspected() throws Throwable {
    @NonNls final String IMPORTS_MODULE_NAME = "ExistingProjectBundleNotInspectedImports";
    @NonNls final String EXPORTS_MODULE_NAME = "ExistingProjectBundleNotInspectedExports";

    Module importsBundleModule = initBundleModule(IMPORTS_MODULE_NAME);
    Module exportsBundleModule = initBundleModule(EXPORTS_MODULE_NAME);

    resetProvider();

    HeaderValuePart headerValue = setBundleHeader(importsBundleModule, ManifestUtils.IMPORT_BUNDLE_HEADER, EXPORTS_MODULE_NAME);

    ProblemsHolder problemsHolder = inspect(headerValue);

    assertNullOrEmpty(problemsHolder.getResults());
  }

  public void testExistingProjectPackageNotInspected() throws Throwable {
    @NonNls final String IMPORTS_MODULE_NAME = "ExistingProjectPackageNotInspectedImports";
    @NonNls final String EXPORTS_MODULE_NAME = "ExistingProjectPackageNotInspectedExports";
    @NonNls final String EXPORTED_PACKAGE_NAME = "exported.package.name";

    Module importsBundleModule = initBundleModule(IMPORTS_MODULE_NAME);
    Module exportsBundleModule = initBundleModule(EXPORTS_MODULE_NAME);

    setBundleHeader(exportsBundleModule, Constants.EXPORT_PACKAGE, EXPORTED_PACKAGE_NAME);

    resetProvider();

    HeaderValuePart headerValue = setBundleHeader(importsBundleModule, Constants.IMPORT_PACKAGE, EXPORTED_PACKAGE_NAME);

    ProblemsHolder problemsHolder = inspect(headerValue);

    assertNullOrEmpty(problemsHolder.getResults());
  }

  public void testNotExportedProjectPackageInspectedAndFixed() throws Throwable {
    @NonNls final String IMPORTS_MODULE_NAME = "NotExportedProjectPackageInspectedImports";
    @NonNls final String EXPORTS_MODULE_NAME = "NotExportedProjectPackageInspectedExports";
    @NonNls final String TO_EXPORT_PACKAGE_NAME = "package2export";

    Module importsBundleModule = initBundleModule(IMPORTS_MODULE_NAME);
    Module exportsBundleModule = initBundleModule(EXPORTS_MODULE_NAME);

    VirtualFile sourceRoot = ModuleRootManager.getInstance(exportsBundleModule).getSourceRoots()[0];
    createChildDirectory(sourceRoot, TO_EXPORT_PACKAGE_NAME);

    resetProvider();

    HeaderValuePart headerValue = setBundleHeader(importsBundleModule, Constants.IMPORT_PACKAGE, TO_EXPORT_PACKAGE_NAME);

    ProblemsHolder problemsHolder = inspect(headerValue);

    ProblemDescriptor problem = doTestProblem(problemsHolder, headerValue, TO_EXPORT_PACKAGE_NAME,
                                              DmServerBundle.message("PackageResolver.problem.message.package-not-exported",
                                                                     TO_EXPORT_PACKAGE_NAME));

    QuickFix fix = assertOneElement(problem.getFixes());
    assertEquals(DmServerBundle.message("PackageResolver.ExportPackageQuickFix.name"), fix.getName());

    fix.applyFix(getProject(), problem);

    ManifestManager.FileWrapper manifest = ManifestManager.getBundleInstance().findManifest(exportsBundleModule);

    String exportsHeaderValue = ManifestUtils.getInstance().getHeaderValue(manifest.getFile(), Constants.EXPORT_PACKAGE);
    assertEquals(exportsHeaderValue, TO_EXPORT_PACKAGE_NAME);
  }

  public void testExistingRepositoryBundleNotInspected() throws Throwable {
    @NonNls final String MODULE_NAME = "ExistingRepositoryBundleNotInspected";
    @NonNls final String USR_BUNDLE_NAME = "usr.bundle";
    @NonNls final String EXT_BUNDLE_NAME = "ext.bundle";

    DMServerInstallation installation = createAndSetActiveInstallation();

    RepositoryPattern usrRepository = null;
    RepositoryPattern extRepository = null;

    for (RepositoryPattern repositoryPattern : installation.collectRepositoryPatterns()) {
      DMServerRepositoryItem repositoryItem = repositoryPattern.getSource();
      if (repositoryItem instanceof DMServerRepositoryWatchedItem) {
        usrRepository = repositoryPattern;
      }
      else if (repositoryItem instanceof DMServerRepositoryExternalItem) {
        extRepository = repositoryPattern;
      }
    }

    createEmptyBundle(usrRepository.findBaseDir().getPath() + "/usrBundle.jar", USR_BUNDLE_NAME);
    createEmptyBundle(extRepository.findBaseDir().getPath() + "/extBundle.jar", EXT_BUNDLE_NAME);

    Module bundleModule = initBundleModule(MODULE_NAME);

    resetProvider();

    {
      HeaderValuePart headerValue = setBundleHeader(bundleModule, ManifestUtils.IMPORT_BUNDLE_HEADER, USR_BUNDLE_NAME);
      ProblemsHolder problemsHolder = inspect(headerValue);
      assertNullOrEmpty(problemsHolder.getResults());
    }

    {
      HeaderValuePart headerValue = setBundleHeader(bundleModule, ManifestUtils.IMPORT_BUNDLE_HEADER, EXT_BUNDLE_NAME);
      ProblemsHolder problemsHolder = inspect(headerValue);
      assertNullOrEmpty(problemsHolder.getResults());
    }
  }

  public void testExistingRepositoryLibraryNotInspected() throws Throwable {
    @NonNls final String MODULE_NAME = "ExistingRepositoryLibraryNotInspected";
    @NonNls final String USR_LIBRARY_NAME = "usr.library";
    @NonNls final String EXT_LIBRARY_NAME = "ext.library";

    DMServerInstallation installation = createAndSetActiveInstallation();

    RepositoryPattern usrRepository = null;
    RepositoryPattern extRepository = null;

    for (RepositoryPattern repositoryPattern : installation.collectRepositoryPatterns()) {
      DMServerRepositoryItem repositoryItem = repositoryPattern.getSource();
      if (repositoryItem instanceof DMServerRepositoryWatchedItem) {
        usrRepository = repositoryPattern;
      }
      else if (repositoryItem instanceof DMServerRepositoryExternalItem) {
        extRepository = repositoryPattern;
      }
    }

    createEmptyLibrary(usrRepository.findBaseDir(), "usrLibrary.libd", USR_LIBRARY_NAME);
    createEmptyLibrary(extRepository.findBaseDir(), "extLibrary.libd", EXT_LIBRARY_NAME);

    Module bundleModule = initBundleModule(MODULE_NAME);

    resetProvider();

    {
      HeaderValuePart headerValue = setBundleHeader(bundleModule, ManifestUtils.IMPORT_LIBRARY_HEADER, USR_LIBRARY_NAME);
      ProblemsHolder problemsHolder = inspect(headerValue);
      assertNullOrEmpty(problemsHolder.getResults());
    }

    {
      HeaderValuePart headerValue = setBundleHeader(bundleModule, ManifestUtils.IMPORT_LIBRARY_HEADER, EXT_LIBRARY_NAME);
      ProblemsHolder problemsHolder = inspect(headerValue);
      assertNullOrEmpty(problemsHolder.getResults());
    }
  }

  private void createEmptyBundle(String path, String symbolicName) throws IOException {
    createMockBundle(path, "Empty Bundle", "1.0.0", symbolicName);
  }

  private void createEmptyLibrary(VirtualFile dir, String fileName, String symbolicName) {
    createMockLibrary(dir, fileName, "Empty Library", "1.0.0", symbolicName);
  }

  @SuppressWarnings("ConstantConditions")
  private HeaderValuePart setBundleHeader(Module bundleModule, String headerName, String headerValueText) {
    ManifestManager.FileWrapper manifest = ManifestManager.getBundleInstance().findManifest(bundleModule);
    ManifestFile manifestFile = manifest.getFile();
    new ManifestUpdater(manifestFile).updateHeader(headerName, headerValueText, false);
    return ((Clause)manifestFile.getHeader(headerName).getHeaderValue()).getValue();
  }
}
