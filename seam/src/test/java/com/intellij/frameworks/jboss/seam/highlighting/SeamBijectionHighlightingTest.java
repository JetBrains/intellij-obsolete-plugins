package com.intellij.frameworks.jboss.seam.highlighting;

import com.intellij.testFramework.builders.JavaModuleFixtureBuilder;
import com.intellij.testFramework.builders.JavaModuleFixtureBuilder.MavenLib;
import org.jetbrains.annotations.NonNls;

public class SeamBijectionHighlightingTest extends SeamHighlightingTestCase {

  @Override
  protected void configureModule(final JavaModuleFixtureBuilder moduleBuilder) throws Exception {
    super.configureModule(moduleBuilder);

    addSeamJar(moduleBuilder);

    if (getName().equals("testContextVariablesTypeMisMatch")) {
      moduleBuilder.addMavenLibrary(new MavenLib("org.hibernate:ejb3-persistence:3.3.2.Beta1"));
      moduleBuilder.addMavenLibrary(new MavenLib("org.hibernate:hibernate-search:3.0.1.GA"));
    }
  }

  public void testIncorrectSignatureBijections() {
    allowTreeAccessForFile("Blog.java", true);

    myFixture.testHighlighting(false, false, true, "IncorrectSignatureBijections.java", "Blog.java");
  }

  public void testUndefinedContextVariables() {
    allowTreeAccessForFile("Blog.java", true);

    myFixture.testHighlighting(true, false, true, "BijectionNonDefinedContextVariable.java", "Blog.java");
  }

  public void testContextVariablesTypeMisMatch() {
    allowTreeAccessForFile("Blog.java", true);
    allowTreeAccessForFile("BlogChild.java", true);

    myFixture.testHighlighting(false, false, true, "BijectionTypeMismatch.java", "Blog.java", "BlogChild.java");
  }

  public void testIllegalScopeDeclaration() {
    allowTreeAccessForFile("Blog.java", true);

    myFixture.testHighlighting(false, false, true, "BijectionIllegalScopeDeclaration.java", "Blog.java");
  }

  @Override
  @NonNls
  public String getBasePath() {
    return super.getBasePath() + "highlighting/";
  }
}
