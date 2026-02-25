// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.groovy.grails;

import com.intellij.psi.PsiFile;

public class GrailsLinkGeneratorTest extends Grails14TestCase {
  public void testCompletion() {
    addController("""
                    
                    class CccController {
                      def index = {}
                      def xxx = {}
                      def yyy = {}
                    }
                    """);

    PsiFile file = addService("""
                                
                                class XxxService {
                                  org.codehaus.groovy.grails.web.mapping.LinkGenerator grailsLinkGenerator
                                
                                  def foo() {
                                    grailsLinkGenerator.link([controller: "ccc", action: '<caret>'])
                                  }
                                }
                                """);
    checkCompletionVariants(file, "index", "xxx", "yyy");
  }

  public void testActionInConditionalOperator() {
    addController("""
                    
                    class CccController {
                      def index = {}
                      def xxx = {}
                      def yyy = {}
                    }
                    """);

    PsiFile file = addService("""
                                
                                class XxxService {
                                  org.codehaus.groovy.grails.web.mapping.LinkGenerator grailsLinkGenerator
                                
                                  def foo() {
                                    grailsLinkGenerator.link([controller: "ccc", action: b ? '<caret>'])
                                  }
                                }
                                """);
    checkCompletionVariants(file, "index", "xxx", "yyy");
  }

  public void testCompletionContextPath() {
    configureByController("""
                            
                            class CccController {
                              org.codehaus.groovy.grails.web.mapping.LinkGenerator grailsLinkGenerator
                            
                              def index() {
                                grailsLinkGenerator.resource(contextPath: "/<caret>")
                              }
                            }
                            """);

    checkCompletion("grails-app", "src");
  }

  public void testCompletionFile() {
    myFixture.addFileToProject("web-app/css/a.css", "");
    myFixture.addFileToProject("web-app/css/b.css", "");

    configureByController("""
                            
                            class CccController {
                              org.codehaus.groovy.grails.web.mapping.LinkGenerator grailsLinkGenerator
                            
                              def index() {
                                grailsLinkGenerator.resource(dir: 'css', file: "<caret>")
                              }
                            }
                            """);

    checkCompletion("a.css", "b.css");
  }

  public void testConditional() {
    myFixture.addFileToProject("web-app/css/a.css", "");
    myFixture.addFileToProject("web-app/css/b.css", "");

    configureByController("""
                            
                            class CccController {
                              org.codehaus.groovy.grails.web.mapping.LinkGenerator grailsLinkGenerator
                            
                              def index() {
                                grailsLinkGenerator.resource(dir: 'css', file: f ? "<caret>" : "zzz.css")
                              }
                            }
                            """);

    checkCompletion("a.css", "b.css");
  }
}
