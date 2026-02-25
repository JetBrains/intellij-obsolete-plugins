// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.groovy.grails;

import com.intellij.codeInsight.daemon.impl.HighlightInfo;
import com.intellij.codeInsight.daemon.impl.analysis.HtmlUnknownTargetInspection;
import com.intellij.codeInsight.daemon.impl.analysis.XmlPathReferenceInspection;
import com.intellij.codeInsight.daemon.impl.analysis.XmlUnresolvedReferenceInspection;
import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.roots.ContentEntry;
import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.roots.OrderRootType;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.openapi.roots.libraries.LibraryTable;
import com.intellij.openapi.roots.libraries.LibraryTablesRegistrar;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.JarFileSystem;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.testFramework.*;
import com.intellij.testFramework.fixtures.DefaultLightProjectDescriptor;
import com.intellij.testFramework.fixtures.JavaCodeInsightTestFixture;
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase;
import com.intellij.util.ArrayUtilRt;
import com.intellij.util.ThrowableRunnable;
import com.intellij.util.containers.ContainerUtil;
import junit.framework.TestCase;import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.grails.structure.GrailsApplicationManager;
import org.jetbrains.plugins.grails.structure.GrailsApplicationProvider;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.GrVariable;
import org.jetbrains.plugins.groovy.mvc.MvcModuleStructureSynchronizer;
import org.jetbrains.plugins.groovy.util.TestUtils;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class GrailsTestCase extends LightJavaCodeInsightFixtureTestCase {

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    myFixture.enableInspections(new XmlPathReferenceInspection(), new HtmlUnknownTargetInspection(), new XmlUnresolvedReferenceInspection());
    GrailsApplicationProvider.APPLICATION_PROVIDER.getPoint().registerExtension(new TestGrailsApplicationProvider(), myFixture.getTestRootDisposable());
    Future<?> futureUpdate = GrailsApplicationManager.getInstance(getProject()).queueUpdate();
    PlatformTestUtil.waitForFuture(futureUpdate, 5 * 60 * 1000 /* 5 minutes */);
  }

  @Override
  protected void tearDown() throws Exception {
    // remove grails paths from project model and dispose leaking virtual file pointers
    try {
      VirtualFile app = ModuleRootManager.getInstance(getModule()).getContentRoots()[0].findChild("grails-app");
      VfsTestUtil.deleteFile(app);
      MvcModuleStructureSynchronizer instance = MvcModuleStructureSynchronizer.getInstance(getProject());
      instance.getFileAndRootsModificationTracker().incModificationCount();
      Future<?> futureUpdate = GrailsApplicationManager.getInstance(getProject()).queueUpdate();
      PlatformTestUtil.waitForFuture(futureUpdate, 5 * 60 * 1000 /* 5 minutes */);
    }
    catch (Throwable e) {
      addSuppressedException(e);
    }
    finally {
      super.tearDown();
    }
  }

  protected boolean useGrails14() {
    return false;
  }

  protected void configureGrails(@NotNull Module module, @NotNull ModifiableRootModel model, ContentEntry contentEntry) {
    if (useGrails14()) {
      configureGrails14(model, contentEntry);
    }
    else {
      configureOldGrails(model, contentEntry);
    }
  }

  private void configureGrails14(@NotNull ModifiableRootModel model, ContentEntry contentEntry) {
    PsiTestUtil.addLibrary(model, "GROOVY", GrailsTestUtil.getMockGrailsLibraryHome(), TestUtils.GROOVY_JAR);

    File[] files = new File(GrailsTestUtil.getMockGrails14LibraryHome()).listFiles();

    List<String> jarsNames = new ArrayList<>();
    final List<String> sourceJarsNames = new ArrayList<>();

    for (File file : files) {
      String name = file.getName();
      assert name.endsWith(".jar");

      if (name.endsWith("-sources.jar")) {
        sourceJarsNames.add(name);
      }
      else {
        jarsNames.add(name);
      }
    }

    PsiTestUtil.addLibrary(model, "Grails14", GrailsTestUtil.getMockGrails14LibraryHome(), jarsNames.toArray(
      ArrayUtilRt.EMPTY_STRING_ARRAY));

    if (includeSources()) {
      ApplicationManager.getApplication().runWriteAction(() -> {
        LibraryTable libraryTable = LibraryTablesRegistrar.getInstance().getLibraryTable(model.getProject());
        Library library = libraryTable.getLibraryByName("Grails14");
        assert library != null;
        Library.ModifiableModel libraryModel = library.getModifiableModel();

        for (String sourceJarsName : sourceJarsNames) {
          VirtualFile root = JarFileSystem.getInstance()
            .refreshAndFindFileByPath(GrailsTestUtil.getMockGrails14LibraryHome() + '/' + sourceJarsName + "!/");
          assert root != null;
          libraryModel.addRoot(root, OrderRootType.SOURCES);
        }

        libraryModel.commit();
      });
    }

    if (needHibernate()) {
      PsiTestUtil.addLibrary(model, "Hibernate", GrailsTestUtil.getMockGrails11LibraryHome(), "/lib/hibernate-core-3.3.1.GA.jar");
    }

    if (needServletApi()) {
      PsiTestUtil.addLibrary(model, "ServletApi", GrailsTestUtil.getMockGrails11LibraryHome(), "/lib/servlet-api-2.5.jar");
    }

    if (needBootstrap()) {
      PsiTestUtil.addLibrary(model, "Bootstrap", GrailsTestUtil.getMockGrails14LibraryHome(), "grails-bootstrap-2.0.0.M2.jar");
    }

    if (needJUnit()) {
      PsiTestUtil.addLibrary(model, "JUnit", GrailsTestUtil.getMockGrails11LibraryHome(), "/lib/junit-4.8.1.jar");
    }
  }

  private void configureOldGrails(@NotNull ModifiableRootModel model, ContentEntry contentEntry) {
    PsiTestUtil.addLibrary(model, "GROOVY", GrailsTestUtil.getMockGrailsLibraryHome(), TestUtils.GROOVY_JAR);

    PsiTestUtil.addLibrary(model, "Grails", GrailsTestUtil.getMockGrails11LibraryHome(), "/dist/grails-core-1.3.1.jar",
                           "/dist/grails-web-1.3.1.jar");

    if (needGormLibrary()) {
      PsiTestUtil.addLibrary(model, "Gorm", GrailsTestUtil.getMockGrails11LibraryHome(), "/dist/grails-gorm-1.3.1.jar");
    }

    if (needHibernate()) {
      PsiTestUtil.addLibrary(model, "Hibernate", GrailsTestUtil.getMockGrails11LibraryHome(), "/lib/hibernate-core-3.3.1.GA.jar");
    }

    if (needTests()) {
      PsiTestUtil.addLibrary(model, "Tests", GrailsTestUtil.getMockGrails11LibraryHome(), "/dist/grails-test-1.3.1.jar", "/lib/org.springframework.test-3.0.2.RELEASE.jar");
    }

    if (needServletApi()) {
      PsiTestUtil.addLibrary(model, "ServletApi", GrailsTestUtil.getMockGrails11LibraryHome(), "/lib/servlet-api-2.5.jar");
    }

    if (needJUnit()) {
      PsiTestUtil.addLibrary(model, "JUnit", GrailsTestUtil.getMockGrails11LibraryHome(), "/lib/junit-4.8.1.jar");
    }

    if (needWebFlow()) {
      PsiTestUtil.addLibrary(model, "WebFlow", GrailsTestUtil.getMockGrails11LibraryHome(), "/dist/grails-webflow-1.3.7.jar");
    }

    if (needScripts()) {
      PsiTestUtil.addLibrary(model, "GrailsScripts", GrailsTestUtil.getMockGrails11LibraryHome(), "/dist/grails-scripts-1.3.7.jar");
    }

    if (needUrlMappings()) {
      PsiTestUtil.addLibrary(model, "GrailsUrlMapping", GrailsTestUtil.getMockGrails11LibraryHome(), "/dist/grails-plugin-url-mappings-2.0.4.jar");
    }
  }

  protected boolean needUrlMappings() {
    return false;
  }

  protected boolean includeSources() {
    return false;
  }

  protected boolean needGormLibrary() {
    return false;
  }

  protected boolean needScripts() {
    return false;
  }

  protected boolean needWebFlow() {
    return false;
  }

  protected boolean needHibernate() {
    return false;
  }

  protected boolean needTests() {
    return false;
  }

  protected boolean needJUnit() {
    return false;
  }

  protected boolean needServletApi() {
    return false;
  }

  protected boolean needBootstrap() {
    return false;
  }

  @NotNull
  @Override
  protected LightProjectDescriptor getProjectDescriptor() {
    return new DefaultLightProjectDescriptor() {
      @Override
      public void configureModule(@NotNull Module module, @NotNull ModifiableRootModel model, @NotNull ContentEntry contentEntry) {
        super.configureModule(module, model, contentEntry);

        configureGrails(module, model, contentEntry);

        try {
          myFixture.getTempDirFixture().findOrCreateDir(".").refresh(false, true);
          GrailsTestUtil.createGrailsApplication(myFixture, module, contentEntry, ".", true);
        }
        catch (IOException e) {
          throw new RuntimeException(e);
        }

        GrailsTestCase.this.configureModule(module, model, contentEntry);
      }
    };
  }

  protected void configureModule(Module module, ModifiableRootModel model, ContentEntry contentEntry) {

  }

  @Override
  public void runTestRunnable(@NotNull ThrowableRunnable<Throwable> testRunnable) throws Throwable {
    MvcModuleStructureSynchronizer.ourGrailsTestFlag = true;
    try {
      super.runTestRunnable(testRunnable);
    }
    finally {
      MvcModuleStructureSynchronizer.ourGrailsTestFlag = false;
    }
  }

  public static void checkResolve(PsiFile file, final String ... expectedUnresolved) {
    TestUtils.checkResolve(file, expectedUnresolved);
  }

  public void checkGutters(PsiFile file, String ... expectedProperties) {
    myFixture.configureFromExistingVirtualFile(file.getVirtualFile());

    String text = file.getText();

    Set<String> actualProperties = new HashSet<>();

    for (HighlightInfo h : myFixture.doHighlighting()) {
      if (h.getGutterIconRenderer() != null) {
        String s = text.substring(h.getStartOffset(), h.getEndOffset());
        actualProperties.add(s);
      }
    }

    TestCase.assertEquals(ContainerUtil.newHashSet(expectedProperties), actualProperties);
  }

  protected void checkCompletion(PsiFile file, String ... expectedVariants) {
    checkCompletionStatic(myFixture, file, expectedVariants);
  }

  protected void checkCompletion(String ... expectedVariants) {
    checkCompletionStatic(myFixture, expectedVariants);
  }

  protected void checkNonExistingCompletionVariants(String ... variants) {
    List<String> list = myFixture.getLookupElementStrings();

    List<String> variantsList = new ArrayList<>(Arrays.asList(variants));
    variantsList.retainAll(list);

    UsefulTestCase.assertEmpty("Some completion variants exist, but should be absence", variantsList);
  }

  public static void checkCompletionStatic(JavaCodeInsightTestFixture fixture, PsiFile file, String ... expectedVariants) {
    TestUtils.checkCompletionContains(fixture, file, expectedVariants);
  }

  public static void checkCompletionStatic(JavaCodeInsightTestFixture fixture, String ... expectedVariants) {
    TestUtils.checkCompletionContains(fixture, expectedVariants);
  }

  protected void runIntention(final PsiFile file, String hint, boolean isAvailable) {
    myFixture.configureFromExistingVirtualFile(file.getVirtualFile());

    List<IntentionAction> intentions = myFixture.filterAvailableIntentions(hint);

    if (!isAvailable) {
      UsefulTestCase.assertEmpty(intentions);
      return;
    }

    UsefulTestCase.assertSize(1, intentions);

    final IntentionAction intention = intentions.get(0);

    TestCase.assertTrue(intention.isAvailable(myFixture.getProject(), myFixture.getEditor(), file));

    myFixture.launchAction(intention);
  }

  public static void checkTypeOfVariable(PsiFile file, String variableName, String type) {
    String text = file.getText();
    int index = text.indexOf(variableName);

    TestCase.assertTrue(index != -1);

    TestCase.assertEquals("Name of variable should occurs once in file", -1, text.indexOf(variableName, index + 1));

    PsiElement element = file.findElementAt(index);
    GrVariable variable = PsiTreeUtil.getParentOfType(element, GrVariable.class);

    TestCase.assertNotNull("Variable '" + variableName + "' not found", variable);

    TestCase.assertEquals(type, variable.getTypeGroovy().getPresentableText());
  }

  public PsiFile addGroovyClass(String path, String classText) {
    return addGroovyClass(path, classText, "");
  }

  @Nullable
  public static Pair<String, String> extractClassAndPackageNames(String classText) {
    String packageName = "";

    Matcher matcher = Pattern.compile("\\s*\\bpackage\\s+([\\w\\$\\.]*[\\w\\$])[\\s;].*", Pattern.DOTALL).matcher(classText);
    if (matcher.matches()) {
      packageName = matcher.group(1);
    }

    matcher = Pattern.compile("\\bclass\\s+([\\w\\$\\.]*[\\w\\$])\\s*(?:\\{|extends\\b|implements\\b)", Pattern.DOTALL).matcher(classText);

    if (!matcher.find()) return null;

    String className = matcher.group(1);

    return Pair.create(packageName, className);
  }

  private PsiFile addGroovyClass(String path, String classText, @NotNull String suffix) {
    if (!path.endsWith("/")) path += '/';

    Pair<String, String> pair = extractClassAndPackageNames(classText);
    assert pair != null;

    String packageName = pair.first;
    String className = pair.second;

    if (!StringUtil.isEmpty(packageName)) {
      path = path + packageName.replace('.', '/') + '/';
    }

    TestCase.assertTrue("Name of class must has suffix " + suffix, className.endsWith(suffix));

    path = path + className + ".groovy";

    return myFixture.addFileToProject(path, classText);
  }

  public PsiFile addDomain(String classText) {
    return addGroovyClass("grails-app/domain/", classText);
  }

  public PsiFile configureByDomain(String classText) {
    PsiFile file = addDomain(classText);
    myFixture.configureFromExistingVirtualFile(file.getVirtualFile());
    return file;
  }

  public PsiFile addController(String classText) {
    return addGroovyClass("grails-app/controllers/", classText, "Controller");
  }

  public PsiFile addSimpleGroovyFile(String fileText) {
    Pair<String, String> pair = extractClassAndPackageNames(fileText);
    if (pair != null) {
      return addGroovyClass("src/groovy", fileText);
    }

    VirtualFile srcGroovy;
    try {
      srcGroovy = myFixture.getTempDirFixture().findOrCreateDir("src/groovy");
    }
    catch (IOException e) {
      throw new UncheckedIOException(e);
    }

    char a = 'A';

    while (srcGroovy.findChild(a + ".groovy") != null) {
      a++;
    }

    return myFixture.addFileToProject("src/groovy/" + a + ".groovy", fileText);
  }

  public PsiFile configureBySimpleGroovyFile(String fileText) {
    PsiFile res = addSimpleGroovyFile(fileText);
    myFixture.configureFromExistingVirtualFile(res.getVirtualFile());
    return res;
  }

  public PsiFile configureByController(String classText) {
    PsiFile file = addController(classText);
    myFixture.configureFromExistingVirtualFile(file.getVirtualFile());
    return file;
  }

  public PsiFile addService(String classText) {
    return addGroovyClass("grails-app/services/", classText, "Service");
  }

  public PsiFile addTaglib(String classText) {
    return addGroovyClass("grails-app/taglib/", classText, "TagLib");
  }

  public PsiFile configureByTaglib(String classText) {
    PsiFile file = addTaglib(classText);
    myFixture.configureFromExistingVirtualFile(file.getVirtualFile());
    return file;
  }

  public void checkCompletionVariants(PsiFile file, String ... variants) {
    myFixture.testCompletionVariants(getFilePath(file), variants);
  }

  public String getFilePath(PsiFile file) {
    VirtualFile virtualFile = file.getVirtualFile();
    assert virtualFile != null;
    VirtualFile root = ProjectRootManager.getInstance(myFixture.getProject()).getFileIndex().getContentRootForFile(virtualFile);
    assert root != null;

    return VfsUtilCore.getRelativePath(virtualFile, root, '/');
  }

  public PsiFile addView(@NotNull String path, String viewText) {
    assert !path.startsWith("/");
    assert path.endsWith(".gsp");
    return myFixture.addFileToProject("grails-app/views/" + path, viewText);
  }

  public PsiFile configureByView(@NotNull String path, String viewText) {
    assert !path.startsWith("/");
    assert path.endsWith(".gsp");
    PsiFile file = myFixture.addFileToProject("grails-app/views/" + path, viewText);
    myFixture.configureFromExistingVirtualFile(file.getVirtualFile());
    return file;
  }
}
