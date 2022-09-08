package com.intellij.frameworks.jboss.seam.highlighting;

import com.intellij.javaee.el.inspections.ELValidationInspection;
import com.intellij.testFramework.builders.JavaModuleFixtureBuilder;
import org.jetbrains.annotations.NonNls;

public class SeamComponentELHighlightingTest extends SeamHighlightingTestCase {

  @Override
  protected void configureModule(final JavaModuleFixtureBuilder moduleBuilder) throws Exception {
    super.configureModule(moduleBuilder);

    addSeamJar(moduleBuilder);

  }

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    myFixture.enableInspections(ELValidationInspection.class);
  }

  public void testUndefinedContextVariables() {
    allowTreeAccessForFile("Blog.java", true);

    myFixture.testHighlighting(true, false, true, "ELUndefinedContextVariables.java", "Blog.java");
  }

  @Override
  @NonNls
  public String getBasePath() {
    return super.getBasePath() + "highlighting/";
  }
}
