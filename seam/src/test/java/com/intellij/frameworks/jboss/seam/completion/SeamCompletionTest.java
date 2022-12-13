package com.intellij.frameworks.jboss.seam.completion;

import com.intellij.codeInsight.completion.impl.CamelHumpMatcher;
import com.intellij.frameworks.jboss.seam.highlighting.SeamHighlightingTestCase;
import com.intellij.testFramework.builders.JavaModuleFixtureBuilder;
import org.jetbrains.annotations.NonNls;

public class SeamCompletionTest extends SeamHighlightingTestCase {

  @Override
  protected void configureModule(final JavaModuleFixtureBuilder moduleBuilder) throws Exception {
    super.configureModule(moduleBuilder);
    moduleBuilder.setMockJdkLevel(JavaModuleFixtureBuilder.MockJdkLevel.jdk15);
    moduleBuilder.addSourceRoot("src");
    
    addSeamJar(moduleBuilder);
  }

  public void testGetterCompletion() {
    myFixture.testCompletion("SeamElwithGetter.java", "SeamElwithGetter_after.java", "SeamCompletedComponent.java");
  }

  public void testAnnotationsWithEL() {
    CamelHumpMatcher.forceStartMatching(myFixture.getTestRootDisposable());
    myFixture.testCompletion("SeamElAnnotaions_1.java", "SeamElAnnotaions_1_after.java", "SeamCompletedComponent.java");

    myFixture.testCompletion("SeamElAnnotaions_2.java", "SeamElAnnotaions_2_after.java");
  }

  public void testObserverEventType() {
    myFixture.copyFileToProject("ObserverOwner.java");
    myFixture.testCompletionVariants("ObserverEventRaiser.java", "eventType_1","eventType_2","eventType_3","eventType_4","<caret>eventType_5");
  }

  public void testObserverAnnotatedEventType() {
    myFixture.copyFileToProject("ObserverOwner.java");

    myFixture.testCompletionVariants("ObserverRaiseEventAnno.java", "eventType_1","eventType_2","eventType_3","eventType_4","<caret>eventType_5");
  }

  public void testObserverRaiserEventType() {
    myFixture.copyFileToProject("ObserverRaiseEventAnno.java");
    myFixture.copyFileToProject("ObserverEventRaiser.java");
    myFixture.testCompletionVariants("ObserverOwner.java", "eventType_1","eventType_2","eventType_3","eventType_4","<caret>eventType_5", "<caret>annoEventType_1", "annoEventType_2");
  }

  @Override
  @NonNls
  public String getBasePath() {
    return super.getBasePath() + "completion/";
  }
}
