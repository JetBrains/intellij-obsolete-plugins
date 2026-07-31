package com.intellij.gwt.model;

import com.intellij.gwt.GwtTestOptions;
import com.intellij.gwt.facet.GwtFacet;
import com.intellij.gwt.psi.GwtLanguageLevelPusher;
import com.intellij.gwt.psi.GwtSourcePathsRefresher;
import com.intellij.gwt.references.GwtCodeInsightTestCase;
import com.intellij.gwt.sdk.impl.GwtVersionImpl;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.pom.java.LanguageLevel;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiResolveHelper;
import com.intellij.psi.util.PsiUtil;
import com.intellij.testFramework.IdeaTestUtil;
import com.intellij.testFramework.IndexingTestUtil;

public class GwtLanguageLevelPusherTest extends GwtCodeInsightTestCase {
  @Override
  protected void setUp() throws Exception {
    super.setUp();
    IdeaTestUtil.setProjectLanguageLevel(myCodeInsightFixture.getProject(), LanguageLevel.JDK_1_8);
  }

  @GwtTestOptions(version = GwtVersionImpl.VERSION_2_5)
  public void testDefault25() {
    Project project = myCodeInsightFixture.getProject();
    PsiResolveHelper resolveHelper = PsiResolveHelper.getInstance(project);

    VirtualFile client  = myCodeInsightFixture.findFileInTempDir("client/Client.java");
    VirtualFile client2 = myCodeInsightFixture.findFileInTempDir("client2/Client2.java");
    VirtualFile server  = myCodeInsightFixture.findFileInTempDir("server/Server.java");

    waitForGwtSourcePathsRefresherToComplete();
    assertEquals(LanguageLevel.JDK_1_6, resolveHelper.getEffectiveLanguageLevel(client));
    assertEquals(LanguageLevel.JDK_1_8, resolveHelper.getEffectiveLanguageLevel(client2));
    assertEquals(LanguageLevel.JDK_1_8, resolveHelper.getEffectiveLanguageLevel(server));
  }

  @GwtTestOptions(version = GwtVersionImpl.VERSION_2_7)
  public void testDefault27() {
    Project project = myCodeInsightFixture.getProject();
    PsiResolveHelper resolveHelper = PsiResolveHelper.getInstance(project);

    VirtualFile client  = myCodeInsightFixture.findFileInTempDir("client/Client.java");
    VirtualFile client2 = myCodeInsightFixture.findFileInTempDir("client2/Client2.java");
    VirtualFile server  = myCodeInsightFixture.findFileInTempDir("server/Server.java");

    waitForGwtSourcePathsRefresherToComplete();
    assertEquals(LanguageLevel.JDK_1_7, resolveHelper.getEffectiveLanguageLevel(client));
    assertEquals(LanguageLevel.JDK_1_8, resolveHelper.getEffectiveLanguageLevel(client2));
    assertEquals(LanguageLevel.JDK_1_8, resolveHelper.getEffectiveLanguageLevel(server));
  }

  @GwtTestOptions(version = GwtVersionImpl.VERSION_2_7)
  public void testOnModuleChange() {
    Project project = myCodeInsightFixture.getProject();
    PsiResolveHelper resolveHelper = PsiResolveHelper.getInstance(project);

    VirtualFile client  = myCodeInsightFixture.findFileInTempDir("client/Client.java");
    VirtualFile client2 = myCodeInsightFixture.findFileInTempDir("client2/Client2.java");
    VirtualFile server  = myCodeInsightFixture.findFileInTempDir("server/Server.java");

    PsiFile modulePsiFile = myCodeInsightFixture.configureByFile("MyModule.gwt.xml");
    myCodeInsightFixture.openFileInEditor(modulePsiFile.getVirtualFile());
    myCodeInsightFixture.getEditor().getCaretModel().moveToOffset(modulePsiFile.getText().indexOf("\"/>"));
    myCodeInsightFixture.type('2');

    PsiDocumentManager.getInstance(project).commitDocument(myCodeInsightFixture.getEditor().getDocument());

    waitForGwtSourcePathsRefresherToComplete();
    assertEquals(LanguageLevel.JDK_1_8, resolveHelper.getEffectiveLanguageLevel(client));
    assertEquals(LanguageLevel.JDK_1_7, resolveHelper.getEffectiveLanguageLevel(client2));
    assertEquals(LanguageLevel.JDK_1_8, resolveHelper.getEffectiveLanguageLevel(server));
  }

  @GwtTestOptions(version = GwtVersionImpl.VERSION_2_7)
  public void testOnConfigurationChange() {
    Project project = myCodeInsightFixture.getProject();
    PsiResolveHelper resolveHelper = PsiResolveHelper.getInstance(project);

    VirtualFile client  = myCodeInsightFixture.findFileInTempDir("client/Client.java");
    VirtualFile client2 = myCodeInsightFixture.findFileInTempDir("client2/Client2.java");
    VirtualFile server  = myCodeInsightFixture.findFileInTempDir("server/Server.java");

    GwtFacet gwtFacet = GwtFacet.getInstance(myCodeInsightFixture.getModule());
    assertNotNull(gwtFacet);

    gwtFacet.getConfiguration().setCompilerParameters("-sourceLevel 1.6");
    GwtLanguageLevelPusher.updateConfigurationAndPush(project, true);

    waitForGwtSourcePathsRefresherToComplete();
    assertEquals(LanguageLevel.JDK_1_6, resolveHelper.getEffectiveLanguageLevel(client));
    assertEquals(LanguageLevel.JDK_1_8, resolveHelper.getEffectiveLanguageLevel(client2));
    assertEquals(LanguageLevel.JDK_1_8, resolveHelper.getEffectiveLanguageLevel(server));

    IdeaTestUtil.setProjectLanguageLevel(myCodeInsightFixture.getProject(), LanguageLevel.JDK_1_9);

    waitForGwtSourcePathsRefresherToComplete();
    assertEquals(LanguageLevel.JDK_1_6, resolveHelper.getEffectiveLanguageLevel(client));
    assertEquals(LanguageLevel.JDK_1_9, resolveHelper.getEffectiveLanguageLevel(client2));
    assertEquals(LanguageLevel.JDK_1_9, resolveHelper.getEffectiveLanguageLevel(server));
  }

  public void testChangeProjectLanguageLevel() {
    myCodeInsightFixture.addClass("class Foo {}");

    IdeaTestUtil.setModuleLanguageLevel(myCodeInsightFixture.getModule(), null);
    IdeaTestUtil.setProjectLanguageLevel(myCodeInsightFixture.getProject(), LanguageLevel.JDK_1_4);
    assertEquals(LanguageLevel.JDK_1_4, PsiUtil.getLanguageLevel(myCodeInsightFixture.findClass("Foo")));

    IdeaTestUtil.setProjectLanguageLevel(myCodeInsightFixture.getProject(), LanguageLevel.JDK_1_8);
    assertEquals(LanguageLevel.JDK_1_8, PsiUtil.getLanguageLevel(myCodeInsightFixture.findClass("Foo")));
  }

  private void waitForGwtSourcePathsRefresherToComplete() {
    Project project = myCodeInsightFixture.getProject();
    waitForGwtSourcePathsRefresher(project);
  }

  @Override
  protected String getBaseDirectoryPath() {
    return "model/languageLevelPusher";
  }
}
