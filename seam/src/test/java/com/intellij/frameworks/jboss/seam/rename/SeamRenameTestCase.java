package com.intellij.frameworks.jboss.seam.rename;

import com.intellij.frameworks.jboss.seam.highlighting.SeamHighlightingTestCase;
import com.intellij.testFramework.builders.JavaModuleFixtureBuilder;
import com.intellij.testFramework.builders.JavaModuleFixtureBuilder.MavenLib;
import com.intellij.testFramework.builders.WebModuleFixtureBuilder;
import org.jetbrains.annotations.NonNls;

public abstract class SeamRenameTestCase extends SeamHighlightingTestCase<WebModuleFixtureBuilder> {

  @Override
  protected Class<WebModuleFixtureBuilder> getModuleFixtureBuilderClass() {
    return WebModuleFixtureBuilder.class;
  }

  @Override
  protected void configureModule(final WebModuleFixtureBuilder moduleBuilder) throws Exception {
    super.configureModule(moduleBuilder);
    moduleBuilder.setMockJdkLevel(JavaModuleFixtureBuilder.MockJdkLevel.jdk15);
    moduleBuilder.addSourceRoot("src");

    moduleBuilder.addWebRoot(myFixture.getTempDirPath(), "/");

    moduleBuilder.addMavenLibrary(new MavenLib("myfaces:myfaces-jsf-api:1.0.9"));
    moduleBuilder.addMavenLibrary(new MavenLib("myfaces:myfaces:1.0.9"));

    addSeamJar(moduleBuilder);
  }

  @Override
  @NonNls
  public String getBasePath() {
    return super.getBasePath() + "rename/";
  }
}
