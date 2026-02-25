// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.groovy.grails.action;

import com.intellij.psi.PsiFile;
import junit.framework.TestCase;
import org.jetbrains.plugins.groovy.grails.Grails14TestCase;

public class GrailsGenerateAction2Test extends Grails14TestCase {
  public void testGenerateFromGsp1() {
    PsiFile ccc = addController("""
                                  class CccController {
                                  
                                  }
                                  """);

    PsiFile gsp = addView("ccc/a.gsp", "<g:link action='xxx<caret>'");
    runIntention(gsp, "Create action", true);
    TestCase.assertEquals("""
                            class CccController {
                            
                                def xxx() {}
                            }
                            """, ccc.getText());
  }

  public void testGenerateFromGsp2() {
    PsiFile ccc = addController("""
                                  class CccController {
                                      def aaa() {
                                      }
                                  }
                                  """);
    PsiFile gsp = addView("ccc/a.gsp", "<g:link action='xxx<caret>'");
    runIntention(gsp, "Create action", true);
    TestCase.assertEquals("""
                            class CccController {
                                def aaa() {
                                }
                            
                                def xxx() {}
                            }
                            """, ccc.getText());
  }

  public void testGenerateFromGsp3() {
    PsiFile ccc = addController("""
                                  class CccController {
                                      def aaa() {
                                      }
                                      static def foo() {
                                      }
                                  }
                                  """);

    PsiFile gsp = addView("ccc/a.gsp", "<g:link action='xxx<caret>'");
    runIntention(gsp, "Create action", true);
    TestCase.assertEquals("""
                            class CccController {
                                def aaa() {
                                }
                            
                                def xxx() {}
                            
                                static def foo() {
                                }
                            }
                            """, ccc.getText());
  }

  public void testGenerateFromGsp4() {
    PsiFile ccc = addController("""
                                  class CccController {
                                      def aaa = {
                                  
                                      }
                                      static def foo() {
                                      }
                                  }
                                  """);

    PsiFile gsp = addView("ccc/a.gsp", "<g:link action='xxx<caret>'");
    runIntention(gsp, "Create action", true);
    TestCase.assertEquals("""
                            class CccController {
                                def aaa = {
                            
                                }
                                def xxx = {}
                            
                                static def foo() {
                                }
                            }
                            """, ccc.getText());
  }
}
