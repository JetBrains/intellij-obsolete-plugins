package com.intellij.frameworks.jboss.seam.highlighting;

import com.intellij.testFramework.builders.JavaModuleFixtureBuilder;
import org.jetbrains.annotations.NonNls;

public class SeamJamComponentHighlightingTest extends SeamHighlightingTestCase {

  @Override
  protected void configureModule(final JavaModuleFixtureBuilder moduleBuilder) throws Exception {
    super.configureModule(moduleBuilder);

    addSeamJar(moduleBuilder);

  }

  public void testIncorrectPsiClassForNameAnnotation() {
    myFixture.testHighlighting(true, false, true, "IncorrectNameAnnotatedClass.java");
  }

  public void testIncorrectDataModelSignature() {
    myFixture.testHighlighting(true, false, true, "IncorrectDataModelSignature.java");
  }

  public void testIncorrectFactorySignature() {
    myFixture.testHighlighting(true, false, true, "IncorrectFactorySignature.java");
  }

  public void testIncorrectUnwrapSignature() {
    myFixture.testHighlighting(true, false, true, "IncorrectUnwrapSignature.java");
  }

  public void testDuplicatedCreateAndDestroyAnno() {
    myFixture.testHighlighting(true, false, true, "DublicatedCreateAndDestroyAnnotations.java");
  }

  public void testIncorrectCreateAndDestroySignature() {
    myFixture.testHighlighting(true, false, true, "IncorrectCreateDestroyAnnoSignature1.java");
    myFixture.testHighlighting(true, false, true, "IncorrectCreateDestroyAnnoSignature2.java");
    myFixture.testHighlighting(true, false, true, "IncorrectCreateDestroyAnnoSignature3.java");
  }

  @Override
  @NonNls
  public String getBasePath() {
    return super.getBasePath() + "highlighting/";
  }

}
