package com.intellij.gwt.references;

import com.intellij.psi.PsiFile;
import com.intellij.testFramework.TestDataFile;
import com.intellij.testFramework.TestDataPath;
import com.intellij.usageView.UsageInfo;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

@TestDataPath("$CONTENT_ROOT/../testData/references/uiBinder/")
public class GwtUiBinderFindUsagesTest extends GwtCodeInsightTestCase {
  public void testFindUiConstructorParameter() {
    assertHasUsagesInFiles("ppp/client/parameters/MyParametrizedWidget.java", "MyPanel.ui.xml", "MyParametrizedWidget.java");
  }

  public void testRenameUiConstructorParameter() {
    myCodeInsightFixture.configureByFile("ppp/client/parameters/MyParametrizedWidget.java");
    myCodeInsightFixture.renameElement(myCodeInsightFixture.getElementAtCaret(), "init", true, true);
    myCodeInsightFixture.checkResultByFile("ppp/client/parameters/MyPanel.ui.xml", "ppp/client/parameters/MyPanel_after.ui.xml", true);
  }

  public void testRenameUiXmlFile() {
    final PsiFile[] files = myCodeInsightFixture.configureByFiles("ppp/client/MyComponent.ui.xml", "ppp/client/MyComponent.java");
    myCodeInsightFixture.renameElement(files[0], "MyComponent2.ui.xml");
    assertEquals("MyComponent2", findClass("ppp.client.MyComponent2").getName());
  }

  public void testRenameUiBinderClass() {
    myCodeInsightFixture.configureByFiles("ppp/client/MyComponent.java", "ppp/client/MyComponent.ui.xml");
    myCodeInsightFixture.renameElement(findClass("ppp.client.MyComponent"), "MyComponent3");
    assertEquals("MyComponent3.ui.xml", myCodeInsightFixture.findFileInTempDir("ppp/client/MyComponent3.ui.xml").getName());
  }

  public void testRenameUiXmlFileInUiTemplateAnnotation() {
    final PsiFile[] files = myCodeInsightFixture.configureByFiles("ppp/client/uiTemplate/rename/MyComponentImpl.java",
                                                                  "ppp/client/uiTemplate/rename/MyComponent.ui.xml");
    myCodeInsightFixture.renameElement(files[1], "MyComponentXXX.ui.xml");
    myCodeInsightFixture.checkResultByFile("ppp/client/uiTemplate/rename/MyComponentImpl_after.java");
  }

  public void testRenameCssClassForResourceInStyleType1() {
    myCodeInsightFixture.configureByFiles("ppp/client/MyCssResource.java", "ppp/client/WithStyleType.ui.xml");
    myCodeInsightFixture.renameElementAtCaret("newName");
    myCodeInsightFixture.checkResultByFile("ppp/client/MyCssResource.java", "ppp/client/MyCssResource_after.java", true);
    myCodeInsightFixture.checkResultByFile("ppp/client/WithStyleType.ui.xml", "ppp/client/WithStyleType_after.ui.xml", true);
  }

  public void testRenameCssClassForResourceInStyleType2() {
    myCodeInsightFixture.configureByFiles("ppp/client/WithStyleType.ui.xml", "ppp/client/MyCssResource.java");
    myCodeInsightFixture.renameElementAtCaret("newName");
    myCodeInsightFixture.checkResultByFile("ppp/client/MyCssResource.java", "ppp/client/MyCssResource_withoutcaret.java", true);
    myCodeInsightFixture.checkResultByFile("ppp/client/WithStyleType.ui.xml", "ppp/client/WithStyleType_after.ui.xml", true);
  }

  private void assertHasUsagesInFiles(@TestDataFile String filePath, String... fileNames) {
    final Collection<UsageInfo> usages = myCodeInsightFixture.testFindUsages(filePath);
    Set<String> actualFiles = new HashSet<>();
    for (UsageInfo usage : usages) {
      final PsiFile file = usage.getFile();
      assertNotNull(file);
      actualFiles.add(file.getName());
    }
    assertSameElements(actualFiles, fileNames);
  }

  @Override
  protected String getBaseDirectoryPath() {
    return "references/uiBinder";
  }
}
