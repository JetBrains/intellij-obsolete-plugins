package com.intellij.lang.puppet.resolve;

import com.intellij.lang.puppet.PuppetTestUtil;
import com.intellij.lang.puppet.project.PuppetEntity;
import com.intellij.lang.puppet.project.PuppetEnvironment;
import com.intellij.lang.puppet.project.PuppetModule;
import com.intellij.lang.puppet.project.PuppetProjectManager;
import com.intellij.lang.puppet.project.PuppetProjectModel;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiPolyVariantReference;
import com.intellij.psi.ResolveResult;
import com.intellij.testFramework.TestFrameworkUtil;
import com.intellij.util.Consumer;
import com.intellij.util.containers.MultiMap;
import com.intellij.util.ui.UIUtil;
import junit.framework.TestSuite;
import org.jetbrains.annotations.NotNull;
import org.junit.Assert;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import static com.intellij.lang.puppet.project.PuppetProjectManager.PUPPET_FILE;
import static com.intellij.lang.puppet.resolve.PuppetResolveTestCase.ResultType.SERIALIZE;
import static com.intellij.lang.puppet.resolve.PuppetResolveTestCase.TestType.ALL;

public class PuppetProjectStructureResolveTestSuite extends PuppetResolveTestCase {
  private static final String MODULE_A = "/moduleA";
  private static final String MODULE_B = "/moduleB";
  private static final String SITE_DIR = "site";
  private static final String SOMETHING = "something";
  private static final String TEST_MODULE = "/testmodule";
  private static final String SOMETHING_DEPENDENCY_PATH = SOMETHING + TEST_MODULE;
  private static final String MODULE_DEPENDENCY_PATH = PuppetModule.DEPENDENCIES_DIR + TEST_MODULE;
  private static final String MODULE_FIXTURE_DEPENDENCY_PATH = PuppetModule.FIXTURES_DEPENDENCIES_DIR + TEST_MODULE;
  private static final String ENV_DEPENDENCY_PATH = PuppetEnvironment.DEFAULT_DEPENDENCIES_DIR + TEST_MODULE;
  private static final String ENV_SITE_DEPENDENCY_PATH = SITE_DIR + TEST_MODULE;
  private static final String ENVIRONMENT_CONF_WITH_SITE = "environment_with_site.conf";
  private static final String ENVIRONMENT_CONF_WITH_SITE_AND_MODULES = "environment_with_site_and_modules.conf";
  private static final String ENVIRONMENT_CONF_WITH_EMPTY_PATH = "environment_with_empty_modulepath.conf";

  @Override
  protected String getBasePath() {
    return super.getBasePath() + "/project";
  }

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    removeMetadataJson();
  }

  public void testDependenciesCrossReferencesInModule() {
    doTestCrossDependencies(PuppetModule.DEPENDENCIES_DIR, () -> {
      addMetadataJson();
      addPuppetfile();  // this is necessary to make scope .dependencies-wide
      waitForModelUpdate();
      assertIsPuppetModule();
    });
  }

  public void testDependenciesCrossReferencesInEnvironment() {
    doTestCrossDependencies(PuppetEnvironment.DEFAULT_DEPENDENCIES_DIR, () -> {
      addEnvironmentConf();
      waitForModelUpdate();
      assertIsPuppetEnvironment();
    });
  }

  public void testRuby22517() {
    assertIsNotPuppetModule();
    myFixture.copyDirectoryToProject("ruby22517/manifests", "manifests");
    addMetadataJson();
    waitForModelUpdate();
    assertIsPuppetModule();
    myFixture.configureFromTempProjectFile("manifests/init.pp");
    PsiElement varDeclaration = myFixture.getElementAtCaret();
    assertNotNull(varDeclaration);
    PsiFile declarationFile = varDeclaration.getContainingFile();
    assertNotNull(declarationFile);
    //noinspection ResultOfMethodCallIgnored
    declarationFile.getNode(); // load tree
  }

  private void doTestCrossDependencies(@NotNull String dependenciesDir, @NotNull Runnable entityConfigurator) {
    assertIsNotPuppetModuleOrEnvironment();
    String moduleARoot = dependenciesDir + MODULE_A;
    myFixture.copyDirectoryToProject("crossDependencies" + MODULE_A, moduleARoot);
    String moduleBRoot = dependenciesDir + MODULE_B;
    myFixture.copyDirectoryToProject("crossDependencies" + MODULE_B, moduleBRoot);
    entityConfigurator.run();
    myFixture.configureFromTempProjectFile(moduleARoot + "/manifests/initA.pp");
    compareSerializedReferencesWithEditor(getAnswersFileName(getTestName(true) + "A"));
    myFixture.configureFromTempProjectFile(moduleBRoot + "/manifests/initB.pp");
    compareSerializedReferencesWithEditor(getAnswersFileName(getTestName(true) + "B"));
  }

  public void testDirectoryIndexUpdateInEnvrionment(){
    assertIsNotPuppetModuleOrEnvironment();
    VirtualFile innerFile = createDirectory(SOMETHING);
    PuppetEntity containingEntity = PuppetProjectManager.getInstance(getProject()).findModuleOrEnvironmentForFile(innerFile);
    assertNull(containingEntity);
    addEnvironmentConf();
    assertIsPuppetEnvironment();
    containingEntity = PuppetProjectManager.getInstance(getProject()).findModuleOrEnvironmentForFile(innerFile);
    assertNotNull(containingEntity);
    assertInstanceOf(containingEntity, PuppetEnvironment.class);
  }

  public void testDirectoryIndexUpdateInModule(){
    assertIsNotPuppetModuleOrEnvironment();
    VirtualFile innerFile = createDirectory(SOMETHING);
    PuppetEntity containingEntity = PuppetProjectManager.getInstance(getProject()).findModuleOrEnvironmentForFile(innerFile);
    assertNull(containingEntity);
    addMetadataJson();
    assertIsPuppetModule();
    containingEntity = PuppetProjectManager.getInstance(getProject()).findModuleOrEnvironmentForFile(innerFile);
    assertNotNull(containingEntity);
    assertInstanceOf(containingEntity, PuppetModule.class);
  }

  public void testDependencyPathRenameInEnvironment() {
    assertIsNotPuppetModuleOrEnvironment();
    addEnvironmentConf();
    assertIsPuppetEnvironment();
    VirtualFile envDependency = createDirectory(ENV_DEPENDENCY_PATH);
    assertIsPuppetModule(ENV_DEPENDENCY_PATH);
    VirtualFile dependencyDir = envDependency.getParent();
    assertNotNull(dependencyDir);
    renameFile(dependencyDir, SOMETHING);
    assertIsNotPuppetModule(SOMETHING_DEPENDENCY_PATH);
    envDependency = getProjectFile(SOMETHING_DEPENDENCY_PATH);
    assertNotNull(envDependency);
    VirtualFile newDependencyDir = envDependency.getParent();
    assertNotNull(newDependencyDir);
    renameFile(newDependencyDir, PuppetEnvironment.DEFAULT_DEPENDENCIES_DIR);
    assertIsPuppetModule(ENV_DEPENDENCY_PATH);
  }

  public void testImplicitModuleCatchingOnEnvironmentCreation() {
    assertIsNotPuppetModuleOrEnvironment();
    createDirectory(ENV_DEPENDENCY_PATH);
    assertIsNotPuppetModule(ENV_DEPENDENCY_PATH);
    addEnvironmentConf();
    assertIsPuppetModule(ENV_DEPENDENCY_PATH);
    addEnvironmentConfWithEmtpyModulePath();
    assertIsNotPuppetModule(ENV_DEPENDENCY_PATH);
    createDirectory(ENV_SITE_DEPENDENCY_PATH);
    assertIsNotPuppetModule(ENV_SITE_DEPENDENCY_PATH);
    addEnvironmentConfWithSiteRoot();
    assertIsNotPuppetModule(ENV_DEPENDENCY_PATH);
    assertIsPuppetModule(ENV_SITE_DEPENDENCY_PATH);
    addEnvironmentConfWithSiteAndModulesRoot();
    assertIsPuppetModule(ENV_DEPENDENCY_PATH);
    assertIsPuppetModule(ENV_SITE_DEPENDENCY_PATH);
    removeEnvironmentConf();
    assertIsNotPuppetModule(ENV_DEPENDENCY_PATH);
    assertIsNotPuppetModule(ENV_SITE_DEPENDENCY_PATH);
  }

  public void testImplicitModuleCatchingOnModuleCreation() {
    assertIsNotPuppetModuleOrEnvironment();
    createDirectory(MODULE_DEPENDENCY_PATH);
    assertIsNotPuppetModule(MODULE_DEPENDENCY_PATH);
    addMetadataJson();
    assertIsPuppetModule(MODULE_DEPENDENCY_PATH);
    createDirectory(MODULE_FIXTURE_DEPENDENCY_PATH);
    assertIsNotPuppetModule(MODULE_FIXTURE_DEPENDENCY_PATH);
    addFixturesYaml();
    assertIsNotPuppetModule(MODULE_DEPENDENCY_PATH);
    assertIsPuppetModule(MODULE_FIXTURE_DEPENDENCY_PATH);
    removeMetadataJson();
    assertIsNotPuppetModule(MODULE_DEPENDENCY_PATH);
    assertIsNotPuppetModule(MODULE_FIXTURE_DEPENDENCY_PATH);
    assertIsNotPuppetModuleOrEnvironment();
  }


  public void testImplicitModuleCreationInEnvironmentWithSiteRoot() {
    assertIsNotPuppetModuleOrEnvironment();
    addEnvironmentConfWithSiteRoot();
    doTestImplicitModuleCreation(SITE_DIR);
  }

  public void testImplicitModuleCreationInEnvironmentWithSiteAndModuleRoots() {
    assertIsNotPuppetModuleOrEnvironment();
    addEnvironmentConfWithSiteAndModulesRoot();
    doTestImplicitModuleCreation(SITE_DIR);
    doTestImplicitModuleCreation(PuppetEnvironment.DEFAULT_DEPENDENCIES_DIR);
  }

  public void testImplicitModuleCreationInEnvironment() {
    assertIsNotPuppetModuleOrEnvironment();
    addEnvironmentConf();
    assertIsPuppetEnvironment();

    doTestImplicitModuleCreation(PuppetEnvironment.DEFAULT_DEPENDENCIES_DIR);
  }

  public void testImplicitModuleCreationInModule() {
    assertIsNotPuppetModuleOrEnvironment();
    addMetadataJson();
    assertIsPuppetModule();
    doTestImplicitModuleCreation(PuppetModule.DEPENDENCIES_DIR);
  }

  public void testImplicitModuleCreationInModuleWithFixtures() {
    assertIsNotPuppetModuleOrEnvironment();
    addMetadataJson();
    assertIsPuppetModule();
    addFixturesYaml();
    doTestImplicitModuleCreation(PuppetModule.FIXTURES_DEPENDENCIES_DIR);
  }

  private void doTestImplicitModuleCreation(String dependencyRoot) {
    VirtualFile implicitModule = createDirectory(dependencyRoot + "/test1");
    waitForModelUpdate();
    PuppetProjectModel projectModel = PuppetProjectModel.getInstance(getProject());
    assertNotNull(dumpRootsStructure(), projectModel.getPuppetModuleOrEnvironment(implicitModule));

    implicitModule = createDirectory(dependencyRoot + "/test2");
    waitForModelUpdate();
    assertNotNull(dumpRootsStructure(), projectModel.getPuppetModuleOrEnvironment(implicitModule));
  }

  public void testEnvironmentWithEmptyConf() {
    watchTargetPathDoesNotContain(ENV_SITE_DEPENDENCY_PATH);
    doTestEnvironmentConfSupport(() -> addEnvironmentConf());
  }

  public void testEnvironmentWithEmptyModulePath() {
    watchTargetPathDoesNotContain(ENV_SITE_DEPENDENCY_PATH);
    watchTargetPathDoesNotContain(ENV_DEPENDENCY_PATH);
    doTestEnvironmentConfSupport(() -> addEnvironmentConfWithEmtpyModulePath());
  }

  public void testEnvironmentWithSiteRoot() {
    watchTargetPathDoesNotContain(ENV_DEPENDENCY_PATH);
    doTestEnvironmentConfSupport(() -> addEnvironmentConfWithSiteRoot());
  }

  public void testEnvironmentWithTwoRoots() {
    doTestEnvironmentConfSupport(() -> addEnvironmentConfWithSiteAndModulesRoot());
  }

  private void doTestEnvironmentConfSupport(@NotNull Runnable configurator) {
    installTestModule(ENV_DEPENDENCY_PATH);
    installTestModule(ENV_SITE_DEPENDENCY_PATH);
    assertIsNotPuppetModuleOrEnvironment();
    configurator.run();
    assertIsPuppetEnvironment();
    doTest("twoRootsTest.pp");
  }

  public void testStubResolve() {
    assertIsNotPuppetModuleOrEnvironment();
    addMetadataJson();
    assertIsPuppetModule();
    String testFile = "builtintest.pp";
    myFixture.copyFileToProject(testFile);
    doTest(testFile);
  }

  public void testKeyFiles() {
    assertIsNotPuppetModuleOrEnvironment();
    addPuppetfile();
    assertIsPuppetEnvironment();
    addEnvironmentConf();
    assertIsPuppetEnvironment();
    removePuppetfile();
    assertIsPuppetEnvironment();
    addMetadataJson();
    assertIsPuppetModule();
    removeEnvironmentConf();
    assertIsPuppetModule();
    addPuppetfile();
    assertIsPuppetModule();
    removeMetadataJson();
    removePuppetfile();
    assertIsNotPuppetModuleOrEnvironment();
  }

  public void testMetaFileRename() {
    assertIsNotPuppetModuleOrEnvironment();
    addPuppetfile();
    assertIsPuppetEnvironment();
    renameFile(PUPPET_FILE, "othername");
    assertIsNotPuppetModuleOrEnvironment();
  }

  public void testRootRename() {
    String testDir = "testenv";
    addEnvironmentConf(testDir);
    assertIsPuppetEnvironment(testDir);
    VirtualFile envDir = getProjectFile(testDir);
    PuppetEntity environment = PuppetProjectModel.getInstance(getProject()).getPuppetModuleOrEnvironment(envDir);
    assertInstanceOf(environment, PuppetEnvironment.class);
    assertEquals(testDir, ((PuppetEnvironment)environment).getMetadata().getName());

    String newName = "somedir";
    renameFile(testDir, newName);
    waitForModelUpdate();
    environment = PuppetProjectModel.getInstance(getProject()).getPuppetModuleOrEnvironment(envDir);
    assertInstanceOf(environment, PuppetEnvironment.class);
    assertEquals(newName, ((PuppetEnvironment)environment).getMetadata().getName());
  }

  public void testMetadata() {
    addMetadataJson();
    removeMetadataJson();
  }

  public void testOrphanFile() {
    doTest();
    assertIsNotPuppetModuleOrEnvironment();
  }

  public void testOrphanFileWithModule() {
    installTestModule();
    doTest();
    assertIsNotPuppetModuleOrEnvironment();
  }

  public void testOrphanFileWithModuleAndDependencies() {
    installTestModule();
    installTestModuleToDependencies();
    doTest();
    assertIsNotPuppetModuleOrEnvironment();
  }

  public void testModuleFile() {
    assertIsNotPuppetModuleOrEnvironment();
    addMetadataJson();
    doTest();
  }

  public void testModuleFileWithDependencies() {
    assertIsNotPuppetModuleOrEnvironment();
    addMetadataJson();
    installTestModuleToDependencies();
    shouldResolveToDependencies();
    doTest();
  }

  public void testModuleFileWithModuleAndDependencies() {
    assertIsNotPuppetModuleOrEnvironment();
    addMetadataJson();
    installTestModule();
    installTestModuleToDependencies();
    shouldNotResolveToDependencies();
    doTest();
  }

  public void testModuleRemovingWatching() {
    assertIsNotPuppetModuleOrEnvironment();
    addMetadataJson();
    installTestModuleToDependencies();
    installTestModule();
    shouldNotResolveToDependencies();
    doTest();
    uninstallTestModule();
    shouldResolveToDependencies();
    doTest();
  }

  public void testModuleCorruptingWatching() {
    assertIsNotPuppetModuleOrEnvironment();
    addMetadataJson();
    installTestModuleToDependencies();
    installTestModule();
    shouldNotResolveToDependencies();
    doTest();
    corruptTestModuleViaCopying();
    shouldResolveToDependencies();
    doTest();
  }

  public void testModuleCorruptingViaChangeWatching() {
    assertIsNotPuppetModuleOrEnvironment();
    addMetadataJson();
    installTestModuleToDependencies();
    installTestModule();
    shouldNotResolveToDependencies();
    doTest();
    corruptTestModuleViaChange();
    shouldResolveToDependencies();
    doTest();
  }

  public void testModuleAlteringWatching() {
    assertIsNotPuppetModuleOrEnvironment();
    addMetadataJson();
    installTestModuleToDependencies();
    installTestModule();
    shouldNotResolveToDependencies();
    doTest();
    alterTestModuleViaCopying();
    shouldResolveToDependencies();
    doTest();
  }

  public void testModuleAlteringViaChangeWatching() {
    assertIsNotPuppetModuleOrEnvironment();
    addMetadataJson();
    installTestModuleToDependencies();
    installTestModule();
    shouldNotResolveToDependencies();
    doTest();
    alterTestModuleViaChange();
    shouldResolveToDependencies();
    doTest();
  }

  private void addEnvironmentConf() {
    addEnvironmentConf("");
  }

  private void addEnvironmentConfWithEmtpyModulePath() {
    addEnvironmentConf(ENVIRONMENT_CONF_WITH_EMPTY_PATH, "");
  }

  private void addEnvironmentConfWithSiteRoot() {
    addEnvironmentConf(ENVIRONMENT_CONF_WITH_SITE, "");
  }

  private void addEnvironmentConfWithSiteAndModulesRoot() {
    addEnvironmentConf(ENVIRONMENT_CONF_WITH_SITE_AND_MODULES, "");
  }

  private void addEnvironmentConf(String targetDir) {
    addEnvironmentConf(PuppetProjectManager.ENVIRONMENT_META_FILE, targetDir);
  }

  private void addEnvironmentConf(String sourceFileName, String targetDir) {
    String targetPath = (targetDir.isEmpty() ? "" : targetDir + '/') + PuppetProjectManager.ENVIRONMENT_META_FILE;
    myFixture.copyFileToProject(sourceFileName, targetPath);
    assertIsPuppetEnvironment(targetDir);
  }

  private void addPuppetfile() {
    myFixture.copyFileToProject(PUPPET_FILE);
    waitForModelUpdate();
  }

  private void removeEnvironmentConf() {
    deleteFile(PuppetProjectManager.ENVIRONMENT_META_FILE);
  }

  private void removePuppetfile() {
    deleteFile(PUPPET_FILE);
  }

  private void addMetadataJson() {
    myFixture.copyFileToProject(PuppetProjectManager.MODULE_META_FILE);
    assertIsPuppetModule(); // for now - module wins
  }

  private void addFixturesYaml() {
    myFixture.copyFileToProject(PuppetModule.FIXTURES_FILE);
  }

  private void removeMetadataJson() {
    assertIsPuppetModule();
    deleteFile(PuppetProjectManager.MODULE_META_FILE);
    assertIsNotPuppetModule();
  }

  private void corruptTestModuleViaCopying() {
    myFixture.copyFileToProject("testmodule_corrupted_metadata.json", getTestModuleMetadataFilePath());
    assertIsNotPuppetModule(getTestModuleDirName());
  }

  private void corruptTestModuleViaChange() {
    updateTestModuleMetaFileContentFrom("testmodule_corrupted_metadata.json");
    assertIsNotPuppetModule(getTestModuleDirName());
  }

  private void alterTestModuleViaCopying() {
    myFixture.copyFileToProject("testmodule_wrong_metadata.json", getTestModuleMetadataFilePath());
    assertIsPuppetModule(getTestModuleDirName());
  }

  private void alterTestModuleViaChange() {
    updateTestModuleMetaFileContentFrom("testmodule_wrong_metadata.json");
    assertIsPuppetModule(getTestModuleDirName());
  }

  private void updateTestModuleMetaFileContentFrom(String sourceFileName) {
    String sourceFilePath = getTestDataPath();
    File sourceFile = FileUtil.findFirstThatExist(sourceFilePath + '/' + sourceFileName);
    assertNotNull("Unable to find source file: " + sourceFilePath, sourceFile);
    VirtualFile sourceVirtualFile = VfsUtil.findFileByIoFile(sourceFile, true);
    assertNotNull("Unable to find source file: " + sourceFilePath, sourceVirtualFile);
    VirtualFile targetFile = getProjectFile(getTestModuleMetadataFilePath());

    ApplicationManager.getApplication().runWriteAction(() -> {
      try {
        byte[] content = sourceVirtualFile.contentsToByteArray();
        targetFile.setBinaryContent(content);
      }
      catch (IOException e) {
        e.printStackTrace();
      }
    });
  }

  private @NotNull VirtualFile getProjectFile(String path) {
    VirtualFile targetFile = myFixture.findFileInTempDir(path);
    assertNotNull("Unable to find project file: " + path, targetFile);
    return targetFile;
  }

  private void assertIsNotPuppetModuleOrEnvironment() {
    assertIsNotPuppetModuleOrEnvironment(".");
  }

  private void assertIsNotPuppetModuleOrEnvironment(String path) {
    assertIsNotPuppetModule(path);
    assertIsNotPuppetEnvironment(path);
  }

  private void assertIsPuppetModule() {
    assertIsPuppetModule(".");
  }

  private void assertIsNotPuppetModule() {
    assertIsNotPuppetModule(".");
  }

  private void assertIsPuppetEnvironment() {
    assertIsPuppetEnvironment(".");
  }

  private void assertIsPuppetModule(String path) {
    assertIsPuppetEntity(path, PuppetModule.class);
  }

  private void assertIsPuppetEnvironment(String path) {
    assertIsPuppetEntity(path, PuppetEnvironment.class);
  }

  private void assertIsNotPuppetModule(String path) {
    assertIsNotPuppetEntity(path, PuppetModule.class);
  }

  private void assertIsNotPuppetEnvironment(String path) {
    assertIsNotPuppetEntity(path, PuppetEnvironment.class);
  }

  private void assertIsPuppetEntity(String path, Class<? extends PuppetEntity> clazz) {
    waitForModelUpdate();
    VirtualFile root = getProjectFile(path);
    PuppetEntity entity = PuppetProjectModel.getInstance(getProject()).getPuppetModuleOrEnvironment(root);
    assertInstanceOf(entity, clazz);
  }

  private void assertIsNotPuppetEntity(String path, Class<? extends PuppetEntity> clazz) {
    waitForModelUpdate();
    VirtualFile root = getProjectFile(path);
    PuppetEntity entity = PuppetProjectModel.getInstance(getProject()).getPuppetModuleOrEnvironment(root);
    if (entity == null) {
      return;
    }
    assertFalse("Got not null entity " + entity + " which is not supposed to be a " + clazz, clazz.isInstance(entity));
  }

  private void shouldResolveToDependencies() {
    clearReferenceCheckers();
    watchTargetPathContains(PuppetModule.DEPENDENCIES_DIR);
  }

  private void shouldNotResolveToDependencies() {
    clearReferenceCheckers();
    watchTargetPathDoesNotContain(PuppetModule.DEPENDENCIES_DIR);
  }

  private void watchTargetPathContains(final String word) {
    addTargetPathsValidator(path -> assertTrue(path + " MUST contain " + word, StringUtil.contains(path, word)));
  }

  private void watchTargetPathDoesNotContain(String word) {
    addTargetPathsValidator(path -> assertTrue(path + " MUST NOT contain " + word, !StringUtil.contains(path, word)));
  }

  private void addTargetPathsValidator(@NotNull Consumer<String> pathsConsumer) {
    addReferenceChecker(reference -> {
      for (ResolveResult resolveResult : reference.multiResolve(false)) {
        PsiElement element = resolveResult.getElement();
        if (element == null) {
          return;
        }

        PsiFile file = element.getContainingFile();
        if (file == null) {
          return;
        }

        VirtualFile virtualFile = file.getVirtualFile();
        if (virtualFile == null) {
          return;
        }

        pathsConsumer.consume(virtualFile.getCanonicalPath());
      }
    });
  }

  private void deleteFile(String fileName) {
    VirtualFile file = getProjectFile(fileName);
    ApplicationManager.getApplication().runWriteAction(new Runnable() {

      @Override
      public void run() {
        try {
          file.delete(this);
        }
        catch (IOException e) {
          e.printStackTrace();
        }
      }
    });
    waitForModelUpdate();
  }

  private void renameFile(String oldName, String newName) {
    renameFile(getProjectFile(oldName), newName);
  }

  private void installTestModuleToDependencies() {
    installTestModule(getTestModuleInDependenciesDirName());
  }

  private void installTestModule() {
    installTestModule(getTestModuleDirName());
  }

  private void uninstallTestModule() {
    deleteFile(getTestModuleDirName());
  }

  private void installTestModule(String targetDir) {
    myFixture.copyDirectoryToProject(getTestModuleDirName(), targetDir);
    assertIsPuppetModule(targetDir);
    assertIsNotPuppetEnvironment(targetDir);
  }

  @Override
  protected void doTest() {
    doTest(getTestFileName());
  }

  protected void doTest(@NotNull String sourceFileName) {
    doTest(ALL, SERIALIZE, PsiPolyVariantReference.class, sourceFileName, getAnswersFileName());
  }

  private @NotNull VirtualFile createDirectory(@NotNull String path) {
    try {
      return myFixture.getTempDirFixture().findOrCreateDir(path);
    }
    catch (IOException e) {
      Assert.fail(e.getMessage());
      return null;
    }
  }

  private void renameFile(@NotNull VirtualFile file, @NotNull String newName) {
    ApplicationManager.getApplication().runWriteAction(() -> {
      try {
        file.rename(this, newName);
      }
      catch (IOException e) {
        Assert.fail(e.getMessage());
      }
    });
    waitForModelUpdate();
  }

  @Override
  protected String getTestFileName() {
    return "sample.code";
  }

  private void waitForModelUpdate() {
    UIUtil.dispatchAllInvocationEvents();
    PuppetProjectManager.getInstance(myFixture.getProject()).waitForScan();
  }

  private String dumpRootsStructure() {
    Pair<Map<VirtualFile, PuppetEntity>, MultiMap<VirtualFile, VirtualFile>> rootsData =
      PuppetProjectModel.getInstance(getProject()).getRootsData();

    synchronized (rootsData.first) {
      StringBuilder buffer = new StringBuilder();

      buffer.append("Explicit roots:\n");
      if (rootsData.first.isEmpty()) {
        buffer.append("\tempty\n");
      }
      else {
        rootsData.first.forEach((key, value) -> buffer.append("\t").append(key).append(" => ").append(value).append("\n"));
      }
      buffer.append("\n");

      buffer.append("Implicit roots:\n");
      if (rootsData.second.isEmpty()) {
        buffer.append("\tempty\n");
      }
      else {
        rootsData.second.entrySet().forEach(entry -> {
          buffer.append("\t").append(entry.getKey()).append(":\n");
          entry.getValue().forEach(file -> buffer.append("\t\t").append(file).append("\n"));
        });
      }

      return buffer.toString();
    }
  }

  private static String getTestModuleMetadataFilePath() {
    return getTestModuleDirName() + "/" + PuppetProjectManager.MODULE_META_FILE;
  }

  private static String getTestModuleDirName() {
    return "testmodule";
  }

  private static String getTestModuleInDependenciesDirName() {
    return PuppetModule.DEPENDENCIES_DIR + "/" + getTestModuleDirName();
  }

  public static TestSuite suite() {
    return TestFrameworkUtil.flattenSuite(PuppetTestUtil.createTestSuiteForVersions(PuppetProjectStructureResolveTestSuite.class));
  }
}