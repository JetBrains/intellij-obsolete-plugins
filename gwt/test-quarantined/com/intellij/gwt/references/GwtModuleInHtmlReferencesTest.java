package com.intellij.gwt.references;

import com.intellij.psi.xml.XmlFile;
import com.intellij.testFramework.TestDataFile;
import com.intellij.testFramework.TestDataPath;

@TestDataPath("$CONTENT_ROOT/../testData/references/moduleRefsInHtml/")
public class GwtModuleInHtmlReferencesTest extends GwtCodeInsightTestCase {
  public void testQualifiedNameInPublic() {
    doTest("src/pub/public/MyModule.html", "MyModule.gwt.xml");
  }

  public void testMeta() {
    doTest("src/pub/public/Meta.html", "MyModule2.gwt.xml");
  }

  public void testQualifiedNameInWeb() {
    doTest("web/MyModule.html", "MyModule.gwt.xml");
  }

  public void testQualifiedNameInSubDir() {
    doTest("web/ppp.MyModule/MyModule.html", "MyModule.gwt.xml");
  }

  public void testQualifiedNameInAnotherDir() {
    doTest("web/dir/MyModule.html", "MyModule.gwt.xml");
  }

  public void testShortNameInWeb() {
    doTest("web/Short.html", "Renamed.gwt.xml");
  }

  public void testShortNameInSubDir() {
    doTest("web/short/Short.html", "Renamed.gwt.xml");
  }

  public void testShortNameInAnotherDir() {
    doTest("web/dir/Short.html", "Renamed.gwt.xml");
  }

  private void doTest(@TestDataFile String filePath, String gwtModuleFileName) {
    final XmlFile xmlFile = assertResolvesTo(filePath, XmlFile.class);
    assertEquals(gwtModuleFileName, xmlFile.getName());
  }

  @Override
  protected String getBaseDirectoryPath() {
    return "references/moduleRefsInHtml";
  }
}
