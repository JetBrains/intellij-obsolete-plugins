// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.groovy.grails.reference.controller;

import com.intellij.psi.PsiFile;
import org.jetbrains.plugins.groovy.grails.GrailsTestCase;

public class GrailsMimePluginTest extends GrailsTestCase {
  public void testResolve() {
    PsiFile file = addController("""
                                   class CccController {
                                     def index = {
                                       withFormat {
                                         html {}
                                         js {}
                                         xml bookList: books
                                         foo(bookList: books)
                                         unresolved(1, 2)
                                       }
                                     }
                                   }
                                   """);
    GrailsTestCase.checkResolve(file, "books", "books", "unresolved");
  }

  public void testWithFormatReturnType() {
    PsiFile file = addController("""
                                   class CccController {
                                     def index = {
                                       withFormat {
                                       }.<caret>
                                     }
                                   }
                                   """);
    checkCompletion(file, "size", "putAll", "containsKey");
  }
}
