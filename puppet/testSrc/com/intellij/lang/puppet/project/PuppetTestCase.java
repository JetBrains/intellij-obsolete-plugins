package com.intellij.lang.puppet.project;

import com.intellij.lang.puppet.OnVersion;
import com.intellij.lang.puppet.PuppetFileType;
import com.intellij.lang.puppet.PuppetLanguage;
import com.intellij.lang.puppet.PuppetTestUtil;
import com.intellij.lang.puppet.ide.libraries.PuppetLibraryUtil;
import com.intellij.lang.puppet.settings.PuppetProjectConfiguration;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import com.intellij.testFramework.fixtures.impl.CodeInsightTestFixtureImpl;
import com.intellij.util.ThrowableRunnable;
import org.jetbrains.annotations.NotNull;

/**
 * A parent for all Puppet tests that should inherit {@link BasePlatformTestCase}.
 * If one wants to run tests for all puppet language versions (kinda cross-product of tests and lang versions), insert in your class
 * <pre>{@code
 *   public static TestSuite suite() {
 *     return PuppetTestUtil.createTestSuiteForVersions(<your class name>.class);
 *   }
 * }</pre>
 * If you want to control the particular language versions for the particular tests, use {@link com.intellij.lang.puppet.OnVersion}
 * annotation.
 */
@OnVersion({PuppetLanguage.Version.PUPPET_3, PuppetLanguage.Version.PUPPET_4})
public abstract class PuppetTestCase extends BasePlatformTestCase {

  private @NotNull PuppetLanguage.Version myLanguageVersion = PuppetLanguage.Version.PUPPET_3;

  @Override
  protected String getTestDataPath() {
    return PuppetTestUtil.getTestDataPath() + getBasePath();
  }

  @Override
  protected void runBare(@NotNull ThrowableRunnable<Throwable> testRunnable) throws Throwable {
    final String initialName = getName();
    setName(truncateTestName(initialName));
    try {
      super.runBare(testRunnable);
    }
    finally {
      setName(initialName);
    }
  }

  public @NotNull PuppetLanguage.Version getLanguageVersion() {
    return myLanguageVersion;
  }

  public void setLanguageVersion(@NotNull PuppetLanguage.Version languageVersion) {
    myLanguageVersion = languageVersion;
  }

  protected void setUpLibraries() {
    String currentDataPath = myFixture.getTestDataPath();
    myFixture.setTestDataPath(PuppetTestUtil.getTestDataPath());
    myFixture.copyDirectoryToProject("lib", getTargetProjectLibrary());
    myFixture.copyFileToProject("misc/metadata.json", "metadata.json");
    myFixture.setTestDataPath(currentDataPath);
    var stubsRoot = PuppetLibraryUtil.getStubsRoot(true);
    assertNotNull("Unable to find stubs root: " + PuppetLibraryUtil.PUPPET_STUBS_ROOT_PATH, stubsRoot);
    CodeInsightTestFixtureImpl.ensureIndexesUpToDate(getProject());
  }

  protected String getTargetProjectLibrary() {
    return "lib";
  }

  @Override
  protected void setUp() throws Exception {
    super.setUp();

    PuppetProjectConfiguration.getInstance(getProject()).setLanguageVersion(myLanguageVersion);
  }

  @Override
  protected void tearDown() throws Exception {
    try {
      Project project = getProject();
      PuppetProjectConfiguration puppetProjectConfiguration = project == null ? null : project.getServiceIfCreated(PuppetProjectConfiguration.class);
      if (puppetProjectConfiguration != null) {
        puppetProjectConfiguration.setLanguageVersion(PuppetLanguage.Version.PUPPET_3);
        PuppetProjectManager.getInstance(project).waitForScan();
      }
    }
    catch (Throwable e) {
      addSuppressedException(e);
    }
    finally {
      super.tearDown();
    }
  }

  protected String getManifestExtension() {
    return PuppetFileType.DEFAULT_EXTENSION;
  }

  protected void configureByManifest(@NotNull String testFileName) {
    configureByManifest("manifests", testFileName);
  }

  protected void configureByManifest(@NotNull String relativePath, @NotNull String testFileName) {
    String targetFileName = testFileName.replace(".code", "." + getManifestExtension());
    VirtualFile file = myFixture.copyFileToProject(testFileName, relativePath +"/"+ targetFileName);
    myFixture.configureFromExistingVirtualFile(file);
  }

  private static String truncateTestName(@NotNull String testName) {
    final int n = testName.indexOf("#");

    if (n >= 0) {
      return testName.substring(0, n);
    }
    return testName;
  }
}
