package com.intellij.gwt.references;

import com.intellij.spring.SpringInspectionsRegistry;
import com.intellij.spring.testFramework.SpringHighlightingTestCase;
import com.intellij.spring.testFramework.SpringLibraryDefinition;
import com.intellij.testFramework.TestDataPath;

@TestDataPath("$CONTENT_ROOT/../testData/highlighting/spring/")
public class GwtSpringXmlHighlightingTest extends GwtCodeInsightTestCase {

  @Override
  protected void setUp() throws Exception {
    super.setUp();

    SpringHighlightingTestCase.addSpringLibrary(myCodeInsightFixture.getModule(), SpringLibraryDefinition.FRAMEWORK_7_0_0);
  }

  //IDEA-89113
  public void testPackageReference() {
    myCodeInsightFixture.configureByFile("web/WEB-INF/applicationContext.xml");
    myCodeInsightFixture.enableInspections(SpringInspectionsRegistry.getInstance().getTestSpringModelInspectionClass());
    myCodeInsightFixture.checkHighlighting(true, false, true);
  }

  @Override
  protected String getBaseDirectoryPath() {
    return "highlighting/spring";
  }
}
