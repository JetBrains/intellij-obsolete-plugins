// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.groovy.grails.reference.controller;

import com.intellij.psi.PsiFile;
import org.jetbrains.plugins.groovy.grails.Grails14TestCase;

public class GrailsActionWithExtensionTest extends Grails14TestCase {
  public void testRename() {
    PsiFile ddd = addController("""
                                  class DddController {
                                    def index() {
                                      link(controller: 'ccc', action: 'foo.xml')
                                    }
                                  }
                                  """);

    PsiFile view = addView("a.gsp", "<g:link controller='ccc' action='foo.xml'>");

    PsiFile ccc = configureByController("""
                                          class CccController {
                                              def foo<caret>() {
                                                withFormat {
                                                  html {}
                                                  xml {}
                                                }
                                              }
                                          
                                              def index = {
                                                redirect(action: 'foo.html')
                                              }
                                          }
                                          """);

    myFixture.renameElementAtCaret("z");

    assertEquals("""
                   class DddController {
                     def index() {
                       link(controller: 'ccc', action: 'z.xml')
                     }
                   }
                   """, ddd.getText());
    assertEquals("<g:link controller='ccc' action='z.xml'>", view.getText());

    assertEquals("""
                   class CccController {
                       def z() {
                         withFormat {
                           html {}
                           xml {}
                         }
                       }
                   
                       def index = {
                         redirect(action: 'z.html')
                       }
                   }
                   """, ccc.getText());
  }

  public void testCompletion() {
    configureByController("""
                            class CccController {
                                def foo() {
                                  withFormat {
                                    html {}
                                    xml {}
                                  }
                                }
                            
                                def index = {
                                  redirect(action: 'fo<caret>.html')
                                }
                            }
                            """);

    myFixture.completeBasic();

    myFixture.checkResult("""
                            class CccController {
                                def foo() {
                                  withFormat {
                                    html {}
                                    xml {}
                                  }
                                }
                            
                                def index = {
                                  redirect(action: 'foo.html')
                                }
                            }
                            """);
  }
}
