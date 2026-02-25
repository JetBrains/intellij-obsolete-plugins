// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.groovy.grails.tests;

import com.intellij.psi.PsiFile;
import org.jetbrains.plugins.groovy.grails.GrailsTestCase;

public class GrailsVariableEnhanceTest extends GrailsTestCase {
  public void testController() {
    addController("""
                    package aaa;
                    class CccController {
                      def xxx = {}
                      def yyy = {}
                      def zzz = {}
                    }
                    """);

    PsiFile file = myFixture.addFileToProject("test/unit/aaa/CccControllerTests.groovy", """
      package aaa;
      class CccControllerTests extends grails.test.ControllerUnitTestCase {
        void testSomething() {
          controller.<caret>
        }
      }
      """);

    checkCompletion(file, "xxx", "yyy", "zzz");
  }

  public void testTagLib() {
    addTaglib("""
                package aaa;
                class CccTagLib {
                  def xxx = {}
                  def yyy = {}
                  def zzz = {}
                }
                """);

    PsiFile file = myFixture.addFileToProject("test/unit/aaa/CccTagLibTests.groovy", """
      package aaa;
      class CccTagLibTests extends grails.test.TagLibUnitTestCase {
        void testSomething() {
          tagLib.<caret>
        }
      }
      """);

    checkCompletion(file, "xxx", "yyy", "zzz");
  }

  public void testContentAsStringField() {
    addController("""
                    package aaa;
                    class CccController {
                      def xxx = {}
                      def yyy = {}
                      def zzz = {}
                    }
                    """);

    PsiFile file = myFixture.addFileToProject("test/unit/aaa/CccControllerTests.groovy", """
      package aaa;
      class CccControllerTests extends grails.test.ControllerUnitTestCase {
        void testSomething() {
          controller.response.<caret>
        }
      }
      """);

    checkCompletion(file, "contentAsString", "contentLength");
  }

  public void testRenderArgsMethods() {
    addController("""
                    package aaa;
                    class CccController {}
                    """);

    PsiFile file = myFixture.addFileToProject("test/unit/aaa/CccControllerTests.groovy", """
      package aaa;
      class CccControllerTests extends grails.test.ControllerUnitTestCase {
        void testSomething() {
          controller.<caret>
        }
      }
      """);

    checkCompletion(file, "renderArgs", "chainArgs", "redirectArgs", "forwardArgs");
  }

  @Override
  protected boolean needServletApi() {
    return true;
  }

  @Override
  protected boolean needTests() {
    return true;
  }
}
