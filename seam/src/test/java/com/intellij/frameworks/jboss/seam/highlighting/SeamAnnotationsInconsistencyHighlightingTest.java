package com.intellij.frameworks.jboss.seam.highlighting;

import com.intellij.testFramework.builders.JavaModuleFixtureBuilder;
import org.jetbrains.annotations.NonNls;

public class SeamAnnotationsInconsistencyHighlightingTest extends SeamHighlightingTestCase {

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

  public void testIllegalSeamAnnotationsOnJavaBean() {
    myFixture.testHighlighting(false, false, true, "JavaBean.java");
  }

  public void testJavaBeans() {
    myFixture.testHighlighting(true, false, true, "SimpleBean.java");
  }

  public void testStatelessBeans() {
    myFixture.testHighlighting(true, false, true, "StatelessBean.java");
  }

  public void testStatefullBeans() {
    myFixture.testHighlighting(true, false, true, "StatefullBean.java");
  }

  public void testEntityBeans() {
    configureEjbDescriptor();

    myFixture.testHighlighting(true, false, true, "SeamEntityBean.java");
  }

  @Override
  @NonNls
  public String getBasePath() {
    return super.getBasePath() + "highlighting/inconsistency/";
  }
}
