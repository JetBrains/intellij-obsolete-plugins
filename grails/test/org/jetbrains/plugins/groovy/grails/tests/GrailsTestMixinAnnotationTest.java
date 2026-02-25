// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.groovy.grails.tests;

import com.intellij.psi.PsiFile;
import org.jetbrains.plugins.groovy.grails.Grails14TestCase;

public class GrailsTestMixinAnnotationTest extends Grails14TestCase {
  public void testArtifactFieldExists() {
    addController("""
                    class DddController {
                      def index() {}
                    }
                    """);

    addDomain("""
                class Ddd {
                    String name;
                }
                """);

    PsiFile testFile = myFixture.addFileToProject("test/unit/TttTest.groovy", """
      @grails.test.mixin.TestFor(DddController)
      @grails.test.mixin.TestMixin(grails.test.mixin.domain.DomainClassUnitTestMixin)
      class TttTest {
        private void xxx() {
          <caret>
        }
      }
      """);

    checkCompletion(testFile, "initializeDatastoreImplementation()", "mockDomain()", "mockFor()", "log", "assertEquals");
  }

  public void testMixinArray() {
    addController("""
                    class DddController {
                      def index() {}
                    }
                    """);

    addDomain("""
                class Ddd {
                    String name;
                }
                """);

    PsiFile testFile = myFixture.addFileToProject("test/unit/TttTest.groovy", """
      @grails.test.mixin.TestFor(DddController)
      @grails.test.mixin.TestMixin([grails.test.mixin.domain.DomainClassUnitTestMixin, grails.test.mixin.web.ControllerUnitTestMixin])
      class TttTest {
        private void xxx() {
          <caret>
        }
      }
      """);
    checkCompletion(testFile, "initializeDatastoreImplementation()", "mockDomain()", "mockFor()", "log", "assertEquals");
    assertEquals(1, myFixture.getLookupElementStrings().stream().filter(e -> "mockController".equals(e)).count());
  }

  @Override
  protected boolean needJUnit() {
    return true;
  }
}
