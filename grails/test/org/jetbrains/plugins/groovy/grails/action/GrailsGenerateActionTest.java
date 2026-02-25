// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.groovy.grails.action;

import com.intellij.psi.PsiFile;
import org.jetbrains.plugins.groovy.grails.GrailsTestCase;
import org.junit.Assert;

public class GrailsGenerateActionTest extends GrailsTestCase {

  public void testGenerateFromGsp1() {
    PsiFile ccc = addController("""
                                  class CccController {
                                  
                                  }
                                  """);

    PsiFile gsp = addView("ccc/a.gsp", "<g:link action='xxx<caret>'");

    runIntention(gsp, "Create action", true);

    Assert.assertEquals("""
                          class CccController {
                          
                              def xxx = {}
                          }
                          """, ccc.getText());
  }

  public void testGenerateFromGsp2() {
    PsiFile ccc = addController("""
                                  class CccController {
                                  
                                  }
                                  """);

    PsiFile gsp = addView("fff/a.gsp", "<g:link controller='ccc' action='xx<caret>x'");

    runIntention(gsp, "Create action", true);

    Assert.assertEquals("""
                          class CccController {
                          
                              def xxx = {}
                          }
                          """, ccc.getText());
  }

  public void testGenerateFromGsp3() {
    PsiFile ccc = addController("""
                                  class CccController {
                                  
                                  }
                                  """);

    PsiFile gsp = addView("ccc/a.gsp", "<% link(action:'xxx<caret>') %>");

    runIntention(gsp, "Create action", true);

    Assert.assertEquals("""
                          class CccController {
                          
                              def xxx = {}
                          }
                          """, ccc.getText());
  }

  public void testNotAnIdentifier() {
    addController("""
                    class CccController {
                    
                    }
                    """);

    PsiFile gsp = addView("fff/a.gsp", "<g:link controller='ccc' action='xx<caret>x asda 0'");

    runIntention(gsp, "Create Action", false);
  }

  public void testGenerateFromController() {
    PsiFile ccc = addController("""
                                  class CccController {
                                    def index = {
                                      redirect(action: 'xxx<caret>')
                                    }
                                  }
                                  """);

    runIntention(ccc, "Create action", true);

    Assert.assertEquals("""
                          class CccController {
                            def index = {
                              redirect(action: 'xxx')
                            }
                              def xxx = {}
                          }
                          """, ccc.getText());
  }

  public void testGenerateFromTaglib() {
    PsiFile ccc = addController("""
                                  class CccController {
                                  
                                  }
                                  """);

    PsiFile taglib = addTaglib("""
                                 class TttTagLib {
                                   def ttt = {
                                     link(action: "<caret>xxx", controller: 'ccc')
                                   }
                                 }
                                 """);

    runIntention(taglib, "Create action", true);

    Assert.assertEquals("""
                          class CccController {
                          
                              def xxx = {}
                          }
                          """, ccc.getText());
  }

  public void testFormatterClosure1() {
    PsiFile ccc = addController("""
                                  class CccController {
                                  
                                      def action0 = {
                                      }
                                  
                                  }
                                  """);

    PsiFile gsp = addView("ccc/a.gsp", "<% link(action:'xxx<caret>') %>");

    runIntention(gsp, "Create action", true);

    Assert.assertEquals("""
                          class CccController {
                          
                              def action0 = {
                              }
                              def xxx = {}
                          
                          }
                          """, ccc.getText());
  }

  public void testFormatterClosure2() {
    PsiFile ccc = addController("""
                                  class CccController {
                                  
                                      def action0 = {
                                      }
                                  
                                      private static def foo() {
                                      }
                                  }
                                  """);

    PsiFile gsp = addView("ccc/a.gsp", "<% link(action:'xxx<caret>') %>");

    runIntention(gsp, "Create action", true);

    Assert.assertEquals("""
                          class CccController {
                          
                              def action0 = {
                              }
                              def xxx = {}
                          
                              private static def foo() {
                              }
                          }
                          """, ccc.getText());
  }
}
