package com.intellij.dmserver.test;

import com.intellij.dmserver.editor.AvailableBundlesProvider;
import com.intellij.dmserver.editor.ExportedUnit;
import com.intellij.dmserver.install.DMServerInstallation;
import com.intellij.dmserver.integration.RepositoryPattern;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DMAvailableBundlesProviderTest extends DMFrameworkTestBase {
  public void testExportedPackages() {
    VirtualFile manifestFile = getTempDir().createVirtualFile("manifest.mf");
    setFileText(manifestFile, "Export-Package: the.package1,the.package2;version=\"1.2.3.beta\"\n");
    PsiDocumentManager.getInstance(getProject()).commitAllDocuments();
    PsiFile manifest = PsiManager.getInstance(getProject()).findFile(manifestFile);

    Map<String, ExportedUnit> exportedPackages = createUnitsMap(AvailableBundlesProvider.getExportedPackages(manifest));
    assertEquals(2, exportedPackages.size());
    assertContainsUnit("the.package1", "0.0.0", exportedPackages);
    assertContainsUnit("the.package2", "1.2.3.beta", exportedPackages);
  }

  public void testRepositoryBundlesCollected() throws Exception {
    DMServerInstallation installation = createAndSetActiveInstallation();
    RepositoryPattern repositoryPattern = installation.collectRepositoryPatterns().get(0);

    createMockBundle(repositoryPattern.findBaseDir().getPath() + "/bundle1.jar", "bundle 1", "1.0.0", "the.bundle1");
    createMockBundle(repositoryPattern.findBaseDir().getPath() + "/bundle2.jar", "bundle 2", "2.3.4.beta", "the.bundle2");

    AvailableBundlesProvider provider = AvailableBundlesProvider.getInstance(getProject());
    provider.resetAll();

    Map<String, ExportedUnit> exportedBundles = createUnitsMap(provider.getBundlesCollector().getAvailableUnits());
    assertEquals(2, exportedBundles.size());
    assertContainsUnit("the.bundle1", "1.0.0", exportedBundles);
    assertContainsUnit("the.bundle2", "2.3.4.beta", exportedBundles);
  }

  public void testRepositoryLibrariesCollected() {
    DMServerInstallation installation = createAndSetActiveInstallation();
    RepositoryPattern repositoryPattern = installation.collectRepositoryPatterns().get(0);

    createMockLibrary(repositoryPattern.findBaseDir(), "library1.libd", "library 1", "1.0.0", "the.library1");
    createMockLibrary(repositoryPattern.findBaseDir(), "library2.libd", "library 2", "2.3.4.beta", "the.library2");

    AvailableBundlesProvider provider = AvailableBundlesProvider.getInstance(getProject());
    provider.resetAll();

    Map<String, ExportedUnit> exportedLibraries = createUnitsMap(provider.getLibrariesCollector().getAvailableUnits());
    assertEquals(2, exportedLibraries.size());
    assertContainsUnit("the.library1", "1.0.0", exportedLibraries);
    assertContainsUnit("the.library2", "2.3.4.beta", exportedLibraries);
  }

  public void testRepositoryPackagesCollected() throws Exception {
    DMServerInstallation installation = createAndSetActiveInstallation();
    RepositoryPattern repositoryPattern = installation.collectRepositoryPatterns().get(0);

    createMockBundle(repositoryPattern.findBaseDir().getPath() + "/bundle1.jar", "bundle 1", "1.0.0", "the.bundle1",
                     "Export-Package: the.package1,the.package2;version=\"1.2.3.beta\"\n");

    AvailableBundlesProvider provider = AvailableBundlesProvider.getInstance(getProject());
    provider.resetAll();

    Map<String, ExportedUnit> exportedPackages = createUnitsMap(provider.getPackagesCollector().getAvailableUnits());
    assertEquals(2, exportedPackages.size());
    assertContainsUnit("the.package1", "0.0.0", exportedPackages);
    assertContainsUnit("the.package2", "1.2.3.beta", exportedPackages);
  }

  private static Map<String, ExportedUnit> createUnitsMap(List<ExportedUnit> exportedBundles) {
    Map<String, ExportedUnit> symbolicName2bundle = new HashMap<>();
    for (ExportedUnit exportedBundle : exportedBundles) {
      symbolicName2bundle.put(exportedBundle.getSymbolicName(), exportedBundle);
    }
    return symbolicName2bundle;
  }

  private static void assertContainsUnit(String symbolicName, String version, Map<String, ExportedUnit> symbolicName2bundle) {
    ExportedUnit exportedUnit = symbolicName2bundle.get(symbolicName);
    assertNotNull(exportedUnit);
    assertEquals(version, exportedUnit.getVersion().toString());
  }
}

