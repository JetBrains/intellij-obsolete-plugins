package com.intellij.frameworks.jboss.seam.highlighting;

import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.testFramework.builders.JavaModuleFixtureBuilder;
import org.jetbrains.annotations.NonNls;

public class SeamDomHighlightingTest extends SeamHighlightingTestCase {
  @Override
  protected void setUp() throws Exception {
    super.setUp();

    addJavaeeSupport();
  }

  @Override
  protected void configureModule(final JavaModuleFixtureBuilder moduleBuilder) throws Exception {
    super.configureModule(moduleBuilder);

    addSeamJar(moduleBuilder);
  }

  public void test_IDEADEV_26145() {
    myFixture.copyFileToProject("FooComponent.java");
    myFixture.copyFileToProject("actions//FooComponentDefinedInComponentsXml.java");

    VirtualFile file = getFile(myFixture.getTempDirPath() + "/FooComponent.java");
    assertNotNull(file);

    myFixture.allowTreeAccessForFile(file);

    myFixture.testHighlighting(true, false, true, "components.xml");
  }

  public void test_IDEADEV_32572() {
    myFixture.copyFileToProject("actions//FooComponentDefinedInComponentsXml.java");
    myFixture.copyFileToProject("components.xml");

    myFixture.testHighlighting(true, false, true, "FooTestComponent.java");
  }

  @Override
  @NonNls
  public String getBasePath() {
    return super.getBasePath() + "highlighting/dom/";
  }

}
