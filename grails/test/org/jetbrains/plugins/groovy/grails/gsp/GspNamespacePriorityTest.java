// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.groovy.grails.gsp;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.psi.PsiElement;
import com.intellij.testFramework.UsefulTestCase;
import org.jetbrains.plugins.grails.lang.gsp.psi.gsp.api.GspFile;
import org.jetbrains.plugins.groovy.grails.GrailsTestCase;
import org.junit.Assert;

public class GspNamespacePriorityTest extends GrailsTestCase {
  public void testTmplFirst() {
    addController("class CccController {}");

    addTaglib("""
                class MyTagLib {
                  static namespace = "tmpl"
                
                  def xxx = {}
                }
                """);

    addView("ccc/_xxx.gsp", "Template Text");
    configureByView("ccc/a.gsp", """      
      <%@ taglib prefix="tmpl" uri="http://java.sun.com/tmpl" %>
      <tmpl:xxx<caret>/>
      """);

    PsiElement element = myFixture.getElementAtCaret();
    UsefulTestCase.assertInstanceOf(element, GspFile.class);
  }

  public void testTagLibBeforeCustomTagsFirst() {
    addTaglib("""
                class MyTagLib {
                  static namespace = "fmt"
                
                  def xxx = {}
                }
                """);

    configureByView("a.gsp", """
      <%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
      <fmt:<caret>/>
      """);

    LookupElement[] lookup = myFixture.completeBasic();
    Assert.assertNotNull(lookup);
    Assert.assertEquals(0, lookup.length);
  }
}
