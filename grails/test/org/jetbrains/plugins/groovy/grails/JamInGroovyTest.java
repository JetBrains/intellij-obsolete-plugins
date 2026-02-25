// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.groovy.grails;

import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiReference;
import com.intellij.testFramework.LightProjectDescriptor;
import com.intellij.testFramework.fixtures.DefaultLightProjectDescriptor;
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase;
import org.jetbrains.annotations.NotNull;
import org.junit.Assert;

public class JamInGroovyTest extends LightJavaCodeInsightFixtureTestCase {
  private static final LightProjectDescriptor SPRING_PROJECT =
    new DefaultLightProjectDescriptor().withRepositoryLibrary("org.springframework:spring-beans:4.3.21.RELEASE"
    ).withRepositoryLibrary("org.springframework:spring-core:4.3.21.RELEASE");

  @Override
  protected @NotNull LightProjectDescriptor getProjectDescriptor() {
    return SPRING_PROJECT;
  }

  public void testJamReference() {
    myFixture.addClass("""
                         package org.springframework.test.context;
                         public @interface ContextConfiguration {
                           String locations();
                         }""");
    PsiFile xml = myFixture.addFileToProject("foo/bar.xml", "");
    myFixture.configureByText("Foo.groovy", """
      @org.springframework.test.context.ContextConfiguration(locations="classpath:/foo/ba<caret>r.xml"
      class Foo {}
      """);
    PsiReference ref = myFixture.getFile().findReferenceAt(myFixture.getEditor().getCaretModel().getOffset());
    Assert.assertEquals(ref.resolve(), xml);
  }
}
