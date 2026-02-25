// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.groovy.grails.reference.controller;

import com.intellij.psi.PsiFile;
import org.jetbrains.plugins.groovy.grails.Grails14TestCase;

public class GrailsAction14Test extends Grails14TestCase {
  public void testCompletion() {
    myFixture.addFileToProject("grails-app/controllers/SuperClass.groovy", """
      class SuperClass {
      
        def index = {}
      
        def yyy(int x, String s) {
      
        }
      
        def getCcc() {
          return {
            render "aaa"
          }
        }
      
        def getYyy() {
          render "aaa"
        }
      
      }
      """);

    PsiFile file = addController("""
                                   class CccController extends SuperClass {
                                     def text = {
                                       redirect controller : 'ccc', action: '<caret>'
                                     }
                                   
                                     def kkk = aaaa;
                                   
                                   
                                     public void xxx() {
                                       render("zzz")
                                     }
                                   
                                     Closure getZzz() {
                                       return {
                                         render("zzz")
                                       }
                                     }
                                   }
                                   """);

    checkCompletionVariants(file, "text", "xxx", "getZzz", "getYyy", "getCcc", "yyy", "index");
  }

  public void testRename() {
    configureByController("""
                            class CccController {
                              def getData<caret>() {
                              }
                            }
                            """);
    PsiFile file = addView("ccc/getData.gsp", "");

    myFixture.renameElementAtCaret("getFooData");

    assertEquals("getFooData.gsp", file.getName());
  }
}
