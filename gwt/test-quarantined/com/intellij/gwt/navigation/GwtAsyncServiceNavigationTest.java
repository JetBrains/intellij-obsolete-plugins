package com.intellij.gwt.navigation;

import com.intellij.codeInsight.daemon.GutterMark;
import com.intellij.codeInsight.navigation.GotoTargetHandler;
import com.intellij.gwt.references.GwtCodeInsightTestCase;
import com.intellij.openapi.editor.markup.GutterIconRenderer;
import com.intellij.openapi.ui.TestDialog;
import com.intellij.openapi.ui.TestDialogManager;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiMethod;
import com.intellij.testFramework.TestApplicationManager;
import com.intellij.testFramework.TestDataPath;
import com.intellij.testFramework.TestDataProvider;
import com.intellij.testFramework.fixtures.CodeInsightTestUtil;
import com.intellij.usages.Usage;
import com.intellij.util.containers.ContainerUtil;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@TestDataPath("$CONTENT_ROOT/../testData/navigation/asyncServices/")
public class GwtAsyncServiceNavigationTest extends GwtCodeInsightTestCase {
  public void testGoToMethodImplementations() {
    PsiFile[] files = myCodeInsightFixture.configureByFiles("client/MyApp.java", "client/MyServiceAsync.java", "client/MyService.java",
                                                            "server/MyServiceImpl.java");
    PsiFile file = files[0];
    GotoTargetHandler.GotoData data = CodeInsightTestUtil.gotoImplementation(myCodeInsightFixture.getEditor(), file);
    assertNotNull(data);
    final PsiElement[] impls = data.targets;
    assertEquals(2, impls.length);
    assertSameElements(Arrays.asList(getContainingClassName(impls[0]), getContainingClassName(impls[1])),
                       "MyService", "MyServiceImpl");
  }

  public void testGoToClassImplementations() {
    myCodeInsightFixture.copyDirectoryToProject("", "");
    PsiFile file = myCodeInsightFixture.configureFromTempProjectFile("client/MyServiceAsync.java");
    GotoTargetHandler.GotoData data = CodeInsightTestUtil.gotoImplementation(myCodeInsightFixture.getEditor(), file);
    assertNotNull(data);
    final PsiElement[] impls = data.targets;
    assertEquals(2, impls.length);
    assertSameElements(Arrays.asList(assertInstanceOf(impls[0], PsiClass.class).getName(), assertInstanceOf(impls[1], PsiClass.class).getName()),
                       "MyService", "MyServiceImpl");
  }

  public void testGoToSuperAsyncMethod() {
    myCodeInsightFixture.configureByFiles("client/MyService.java", "client/MyServiceAsync.java");
    List<GutterMark> gutterMarks = myCodeInsightFixture.findGuttersAtCaret();
    List<GutterIconRenderer> renderers = ContainerUtil.filterIsInstance(gutterMarks, GutterIconRenderer.class);
    String tooltips =
      gutterMarks.stream()
        .map(mark -> mark.getTooltipText())
        .collect(Collectors.joining("\n"));
    assertTrue(tooltips, tooltips.contains("Overrides method") && tooltips.contains("MyServiceAsync"));
  }

  public void testFindUsagesOfSyncMethod() {
    TestApplicationManager.getInstance().setDataProvider(TestDataProvider.withRules(myProjectFixture.getProject()), getTestRootDisposable());
    TestDialog oldValue = TestDialogManager.setTestDialog(TestDialog.OK);
    try {
      Collection<Usage> usages =
        myCodeInsightFixture.testFindUsagesUsingAction("client/MyService.java", "client/MyServiceAsync.java", "client/MyApp.java");
      assertOneElement(usages);
    }
    finally {
      TestDialogManager.setTestDialog(oldValue);
    }
  }

  private static String getContainingClassName(PsiElement impl) {
    PsiClass aClass = assertInstanceOf(impl, PsiMethod.class).getContainingClass();
    assertNotNull(aClass);
    return aClass.getName();
  }

  @Override
  protected String getBaseDirectoryPath() {
    return "navigation/asyncServices";
  }
}
