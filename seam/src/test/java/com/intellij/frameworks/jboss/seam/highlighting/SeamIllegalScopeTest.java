package com.intellij.frameworks.jboss.seam.highlighting;

import com.intellij.testFramework.builders.JavaModuleFixtureBuilder;
import org.jetbrains.annotations.NonNls;

public class SeamIllegalScopeTest extends SeamHighlightingTestCase {
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



  public void testStatelessBeans() {
    myFixture.testHighlighting(true, false, true, "StatelessBean.java");
    myFixture.testHighlighting(true, false, true, "StatelessBean2.java");
  }

  public void testStatefullBeans() {
    myFixture.testHighlighting(true, false, true, "StatefullBean.java");
  }

  public void testEntityBeans() {
    configureEjbDescriptor();

    myFixture.testHighlighting(true, false, true, "SeamEntityBean.java");
  }

   public void testDataModels() {
    myFixture.testHighlighting(true, false, true, "DataModelBean.java");
  }

  @Override
  @NonNls
  public String getBasePath() {
    return super.getBasePath() + "highlighting/scopes/";
  }

}
