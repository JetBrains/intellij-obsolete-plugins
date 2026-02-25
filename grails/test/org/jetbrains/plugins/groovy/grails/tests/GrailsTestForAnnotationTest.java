// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.groovy.grails.tests;

import com.intellij.psi.PsiFile;
import com.intellij.psi.impl.light.LightElement;
import org.jetbrains.plugins.groovy.grails.Grails14TestCase;
import org.jetbrains.plugins.groovy.lang.psi.impl.statements.GrFieldImpl;

public class GrailsTestForAnnotationTest extends Grails14TestCase {
  public void testArtifactFieldExists() {
    addDomain("""
                class Ddd {
                    String name;
                }
                """);

    PsiFile testFile = myFixture.addFileToProject("test/unit/TttTest.groovy", """
      import grails.test.mixin.*
      @TestFor(Ddd)
      class TttTest {
        private Ddd domain;
        private void xxx() {
          domai<caret>
        }
      }
      """);

    myFixture.configureFromExistingVirtualFile(testFile.getVirtualFile());

    myFixture.completeBasic();
    myFixture.type("\n");
    assertTrue(myFixture.getElementAtCaret() instanceof GrFieldImpl);
  }

  public void testArtifactFieldNotExists() {
    addDomain("""
                class Ddd {
                    String name;
                }
                """);

    PsiFile testFile = myFixture.addFileToProject("test/unit/TttTest.groovy", """
      import grails.test.mixin.*
      @TestFor(Ddd)
      class TttTest {
        private void xxx() {
          domai<caret>
        }
      }
      """);

    myFixture.configureFromExistingVirtualFile(testFile.getVirtualFile());

    myFixture.completeBasic();
    myFixture.type('\n');
    assertTrue(myFixture.getElementAtCaret() instanceof LightElement);
  }

  public void testMethodFromMixinClasses() {
    addController("""
                    class CccController {
                    }
                    """);

    PsiFile testFile = myFixture.addFileToProject("test/unit/TttTest.groovy", """
      import grails.test.mixin.*
      @TestFor(CccController)
      class TttTest {
        private void xxx() {
          <caret>
        }
      }
      """);
    checkCompletion(testFile, "model", "view", "configureGrailsWeb", "shouldFail", "assertEquals");
  }

  @Override
  protected boolean needJUnit() {
    return true;
  }
}
