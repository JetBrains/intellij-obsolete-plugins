// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.groovy.grails.reference.controller;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.psi.PsiFile;
import org.jetbrains.plugins.groovy.grails.GrailsTestCase;

import java.util.List;

public class GrailsControllerLayoutReferenceTest extends GrailsTestCase {
  public void testCompletion() {
    addView("layouts/ttt/main.gsp", "");
    addView("layouts/aaa.gsp", "");

    configureByController("""
                            class CccController {
                              static layou<caret>
                            }
                            """);

    LookupElement[] c1 = myFixture.completeBasic();
    assertNull(c1);

    myFixture.completeBasic();
    List<String> res = myFixture.getLookupElementStrings();
    assertSameElements(res, "aaa", "ttt");
  }

  public void testMove() {
    addView("layouts/ttt/fff/main.gsp", "");

    PsiFile ccc = addController("""
                                  class CccController {
                                    static layout = 'ttt/fff/main'
                                  }
                                  """);

    myFixture.moveFile("grails-app/views/layouts/ttt/fff/main.gsp", "grails-app/views/layouts/ttt");

    assertEquals("""
                   class CccController {
                     static layout = 'ttt/main'
                   }
                   """, ccc.getText());
  }
}
