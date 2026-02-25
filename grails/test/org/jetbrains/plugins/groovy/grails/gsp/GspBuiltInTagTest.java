// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.groovy.grails.gsp;

import com.intellij.openapi.editor.markup.RangeHighlighter;
import com.intellij.psi.PsiElement;
import com.intellij.testFramework.UsefulTestCase;
import com.intellij.usageView.UsageInfo;
import org.jetbrains.plugins.groovy.grails.GrailsTestCase;

import java.util.HashSet;
import java.util.Set;

public class GspBuiltInTagTest extends GrailsTestCase {
  public void testFindUsages() {
    myFixture.addFileToProject("grails-app/views/a.gsp", """
      <g:if test="true">
        TRUE
      </g:if>
      <g:if<caret> test="false">
        FALSE
      </g:if>
      """);
    myFixture.addFileToProject("grails-app/views/b.gsp", """
      <g:if test="true">
        TRUE
      </g:if>
      """);
    Set<PsiElement> usages = new HashSet<PsiElement>();
    for (UsageInfo u : myFixture.testFindUsages("grails-app/views/a.gsp")) {
      usages.add(u.getElement());
    }


    UsefulTestCase.assertSize(3, usages);
  }

  public void testHighlightUsages() {
    myFixture.addFileToProject("grails-app/views/a.gsp", """
      <g:if test="true">
        TRUE
      </g:if>
      <g:if<caret> test="false">
        FALSE
      </g:if>
      """);
    RangeHighlighter[] usages = myFixture.testHighlightUsages("grails-app/views/a.gsp");
    UsefulTestCase.assertSize(4, usages);
  }
}
