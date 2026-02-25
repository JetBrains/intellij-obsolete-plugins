// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.groovy.grails.reference.taglib;

import com.intellij.psi.PsiFile;
import junit.framework.TestCase;
import org.jetbrains.plugins.groovy.grails.GrailsTestCase;

public class GrailsTagLibNamedArgumentsTest extends GrailsTestCase {
  public void testCompletion() {
    PsiFile file = addController("""
                                   class CccController {
                                     def index = {
                                       link(<caret>)
                                     }
                                   }
                                   """);

    checkCompletion(file, "controller", "uri", "url", "ondblclick");
  }

  public void testResolve() {
    configureByController("""
                            class CccController {
                              def index = {
                                link(controlle<caret>r: 'ccc')
                              }
                            }
                            """);

    TestCase.assertNotNull(myFixture.getElementAtCaret());
  }

  public void testCustomTag() {
    addTaglib("""
                class MyTagLib {
                  def xxx = {attr ->
                    out << attr.aaa << attr.bbb
                  }
                }
                """);

    PsiFile file = addController("""
                                   class CccController {
                                     def index = {
                                       xxx(<caret>)
                                     }
                                   }
                                   """);

    checkCompletion(file, "aaa", "bbb");
  }
}
