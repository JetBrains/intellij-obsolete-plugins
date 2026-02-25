// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.groovy.grails.reference.controller;

import com.intellij.psi.PsiFile;
import org.jetbrains.plugins.grails.references.controller.ControllerAllowedMethodReferenceProvider;
import org.jetbrains.plugins.groovy.grails.GrailsTestCase;

public class GrailsControllerAllowedMethodsTest extends GrailsTestCase {
  public void testCompletionAction1() {
    configureByController("""
                            class CccController {
                              def index1 = {}
                              def index2 = {}
                            
                              static allowedMethods = [<caret>]
                            }
                            """);

    checkCompletion("index1", "index2");
  }

  public void testCompletionAction2() {
    configureByController("""
                            class CccController {
                              def index1 = {}
                              def index2 = {}
                            
                              static allowedMethods = [index<caret>: 'GET']
                            }
                            """);

    checkCompletion("index1", "index2");
  }

  public void testRenameAction() {
    configureByController("""
                            class CccController {
                              def index<caret> = {}
                            
                              static allowedMethods = [index: 'GET']
                            }
                            """);

    myFixture.renameElementAtCaret("xxx");

    myFixture.checkResult("""
                            class CccController {
                              def xxx = {}
                            
                              static allowedMethods = [xxx: 'GET']
                            }
                            """);
  }

  public void testCompletionValue() {
    PsiFile file = addController("""
                                   class CccController {
                                     def index = {}
                                   
                                     static allowedMethods = [index: '<caret>']
                                   }
                                   """);

    myFixture.testCompletionVariants(getFilePath(file), ControllerAllowedMethodReferenceProvider.HTTP_METHODS);
  }

  public void testCompletionValueList() {
    configureByController("""
                            class CccController {
                              def index = {}
                            
                              static allowedMethods = [index: ['<caret>', 'GET', "POST"]]
                            }
                            """);
    checkCompletion("DELETE");
    checkNonExistingCompletionVariants("GET", "POST");
  }
}
