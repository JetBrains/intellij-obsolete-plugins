// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.groovy.grails;

import com.intellij.codeInsight.daemon.impl.HighlightInfo;
import com.intellij.codeInsight.daemon.impl.analysis.HtmlUnknownTargetInspection;
import com.intellij.codeInsight.daemon.impl.analysis.XmlPathReferenceInspection;
import com.intellij.codeInsight.daemon.impl.analysis.XmlUnresolvedReferenceInspection;
import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.facet.Facet;
import com.intellij.facet.FacetManager;
import com.intellij.facet.ModifiableFacetModel;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiRecursiveElementVisitor;
import com.intellij.testFramework.PlatformTestUtil;
import com.intellij.testFramework.builders.JavaModuleFixtureBuilder;
import com.intellij.testFramework.fixtures.JavaCodeInsightFixtureTestCase;
import com.intellij.testFramework.fixtures.JavaCodeInsightTestFixture;
import com.intellij.util.Consumer;
import com.intellij.util.ThrowableRunnable;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.ui.EdtInvocationManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.grails.config.GrailsFramework;
import org.jetbrains.plugins.grails.structure.GrailsApplicationManager;
import org.jetbrains.plugins.grails.structure.GrailsApplicationProvider;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrExpression;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrReferenceExpression;
import org.jetbrains.plugins.groovy.mvc.MvcModuleStructureSynchronizer;
import org.jetbrains.plugins.groovy.mvc.MvcProjectStructure;
import org.jetbrains.plugins.groovy.util.TestUtils;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Future;

public abstract class HddGrailsTestCase extends JavaCodeInsightFixtureTestCase {
  protected void updateApplications() {
    Future<?> futureUpdate = GrailsApplicationManager.getInstance(getProject()).queueUpdate();
    PlatformTestUtil.waitForFuture(futureUpdate, 5 * 60 * 1000 /* 5 minutes */);
  }

  @Override
  protected void tuneFixture(JavaModuleFixtureBuilder moduleBuilder) {
    moduleBuilder.addLibrary("GROOVY", GrailsTestUtil.getMockGrailsLibraryHome() + '/' + TestUtils.GROOVY_JAR);
    moduleBuilder.addLibraryJars("Grails", GrailsTestUtil.getMockGrails11LibraryHome(), "/dist/grails-core-1.3.1.jar",
                                 "/dist/grails-web-1.3.1.jar");
  }

  protected VirtualFile saveProperties(String text) {
    return saveFile("application.properties", text);
  }

  protected VirtualFile saveFile(String path, String text) {
    VirtualFile file = myFixture.getTempDirFixture().getFile(path);
    myFixture.saveText(file, text);
    PsiDocumentManager.getInstance(getProject()).commitAllDocuments();
    return file;
  }

  @Override
  public void runTestRunnable(@NotNull ThrowableRunnable<Throwable> testRunnable) throws Throwable {
    final Ref<Throwable> exc = new Ref<>();

    try {
      EdtInvocationManager.invokeAndWaitIfNeeded(() -> {
        MvcModuleStructureSynchronizer.ourGrailsTestFlag = true;
        try {
          super.runTestRunnable(testRunnable);
        }
        catch (Throwable throwable) {
          exc.set(throwable);
        }
        finally {
          MvcModuleStructureSynchronizer.ourGrailsTestFlag = false;
        }
      });
    }
    catch (Exception e) {
      exc.set(e);
      //suppress
    }

    if (!exc.isNull()) {
      throw exc.get();
    }
  }

  public static void checkResolve(PsiFile file, final String... expectedUnresolved) {
    final List<String> actualUnresolved = new ArrayList<>();

    final StringBuilder sb = new StringBuilder();
    final String text = file.getText();
    final Ref<Integer> lastUnresolvedRef = Ref.create(0);

    file.acceptChildren(new PsiRecursiveElementVisitor() {
      @Override
      public void visitElement(@NotNull PsiElement element) {
        if (element instanceof GrReferenceExpression psiReference) {

          GrExpression qualifier = psiReference.getQualifierExpression();
          if (qualifier instanceof GrReferenceExpression) {
            if (((GrReferenceExpression)qualifier).resolve() == null) {
              super.visitElement(element);
              return;
            }
          }

          if (psiReference.resolve() == null) {
            PsiElement nameElement = psiReference.getReferenceNameElement();
            assert nameElement != null;

            String name = nameElement.getText();

            assert name.equals(psiReference.getReferenceName());

            int last = lastUnresolvedRef.get();
            sb.append(text, last, nameElement.getTextOffset());
            sb.append('!').append(name).append('!');
            lastUnresolvedRef.set(nameElement.getTextOffset() + nameElement.getTextLength());

            actualUnresolved.add(name);
            return;
          }
        }

        super.visitElement(element);
      }

      @Override
      public void visitFile(@NotNull PsiFile psiFile) {
      }
    });

    sb.append("\n\n");

    assertEquals(sb.toString(), Arrays.asList(expectedUnresolved), actualUnresolved);
  }

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    GrailsTestUtil.createGrailsApplication(myFixture);
    myFixture.enableInspections(new XmlPathReferenceInspection(), new HtmlUnknownTargetInspection(), new XmlUnresolvedReferenceInspection());
    GrailsApplicationProvider.APPLICATION_PROVIDER.getPoint()
      .registerExtension(new TestGrailsApplicationProvider(), myFixture.getTestRootDisposable());
    Future<?> futureUpdate = GrailsApplicationManager.getInstance(getProject()).queueUpdate();
    PlatformTestUtil.waitForFuture(futureUpdate, 5 * 60 * 1000 /* 5 minutes */);
  }

  public void checkGutters(PsiFile file, String... expectedProperties) {
    myFixture.configureFromExistingVirtualFile(file.getVirtualFile());

    String text = file.getText();

    Set<String> actualProperties = new HashSet<>();

    for (HighlightInfo h : myFixture.doHighlighting()) {
      if (h.getGutterIconRenderer() != null) {
        String s = text.substring(h.getStartOffset(), h.getEndOffset());
        actualProperties.add(s);
      }
    }

    assertEquals(ContainerUtil.newHashSet(expectedProperties), actualProperties);
  }

  protected void checkCompletion(PsiFile file, String... expectedVariants) {
    checkCompletionStatic(myFixture, file, expectedVariants);
  }

  public static void checkCompletionStatic(JavaCodeInsightTestFixture fixture, PsiFile file, String... expectedVariants) {
    fixture.configureFromExistingVirtualFile(file.getVirtualFile());

    LookupElement[] lookupElements = fixture.completeBasic();

    assertNotNull(lookupElements);

    Set<String> missedVariants = ContainerUtil.newHashSet(expectedVariants);

    for (LookupElement lookupElement : lookupElements) {
      missedVariants.remove(lookupElement.getLookupString());
    }

    assertEmpty("Some completion variants are missed", missedVariants);
  }

  protected void checkNonExistingCompletionVariants(String... variants) {
    List<String> list = myFixture.getLookupElementStrings();

    List<String> variantsList = new ArrayList<>(Arrays.asList(variants));
    variantsList.retainAll(list);

    assertEmpty("Some completion variants exist, but should be absence", variantsList);
  }


  protected void runIntention(final PsiFile file, String hint, boolean isAvailable) {
    myFixture.configureFromExistingVirtualFile(file.getVirtualFile());

    List<IntentionAction> intentions = myFixture.filterAvailableIntentions(hint);

    if (!isAvailable) {
      assertEmpty(intentions);
      return;
    }

    assertSize(1, intentions);

    final IntentionAction intention = intentions.get(0);

    assertTrue(intention.isAvailable(myFixture.getProject(), myFixture.getEditor(), file));

    WriteCommandAction.runWriteCommandAction(null, () -> intention.invoke(myFixture.getProject(), myFixture.getEditor(), file));
  }

  public PsiFile addSimpleGroovyFile(String fileText) {
    Pair<String, String> pair = GrailsTestCase.extractClassAndPackageNames(fileText);
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

  public PsiFile addGroovyClass(String path, String classText) {
    return addGroovyClass(path, classText, "");
  }

  private PsiFile addGroovyClass(String path, String classText, @NotNull String suffix) {
    if (!path.endsWith("/")) path += '/';

    Pair<String, String> pair = GrailsTestCase.extractClassAndPackageNames(classText);
    assert pair != null;

    String packageName = pair.first;
    String className = pair.second;

    if (!StringUtil.isEmpty(packageName)) {
      path = path + packageName.replace('.', '/') + '/';
    }

    assertTrue("Name of class must has suffix " + suffix, className.endsWith(suffix));

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

  public PsiFile configureByController(String classText) {
    PsiFile file = addController(classText);
    myFixture.configureFromExistingVirtualFile(file.getVirtualFile());
    return file;
  }

  public PsiFile addTaglib(String classText) {
    return addGroovyClass("grails-app/taglib/", classText, "TagLib");
  }

  public void checkCompletionVariants(PsiFile file, String... variants) {
    VirtualFile virtualFile = file.getVirtualFile();
    assert virtualFile != null;
    VirtualFile root = ProjectRootManager.getInstance(myFixture.getProject()).getFileIndex().getContentRootForFile(virtualFile);
    assert root != null;

    String path = VfsUtilCore.getRelativePath(virtualFile, root, '/');
    myFixture.testCompletionVariants(path, variants);
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

  protected void setupFacets() {
    ApplicationManager.getApplication().runWriteAction(() -> {
      MvcProjectStructure structure = GrailsFramework.getInstance().createProjectStructure(myFixture.getModule(), false);
      final List<Consumer<ModifiableFacetModel>> actions = new ArrayList<>();
      structure.setupFacets(actions, Collections.singleton(GrailsFramework.getInstance().findAppRoot(myFixture.getModule())));
      final ModifiableFacetModel model = FacetManager.getInstance(myFixture.getModule()).createModifiableModel();
      for (Facet<?> facet : model.getAllFacets()) {
        model.removeFacet(facet);
      }
      for (Consumer<ModifiableFacetModel> action : actions) {
        action.consume(model);
      }
      model.commit();
    });
  }
}
