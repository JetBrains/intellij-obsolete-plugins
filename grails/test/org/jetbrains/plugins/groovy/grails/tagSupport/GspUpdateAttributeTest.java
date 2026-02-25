// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.groovy.grails.tagSupport;

import com.intellij.psi.PsiFile;
import org.jetbrains.plugins.groovy.grails.Grails14TestCase;

public class GspUpdateAttributeTest extends Grails14TestCase {
  public void testXmlAttr() {
    PsiFile view = addView("a.gsp", """
      <div id="ddd"> </div>
      <g:link id="lll">sss</g:link>
      
      <g:remoteLink update="<caret>">s</g:remoteLink>
      """);

    checkCompletionVariants(view, "ddd", "lll");
  }

  public void testGroovyAttr() {
    PsiFile view = addView("a.gsp", """
      <div id="ddd"> </div>
      <g:link id="lll">sss</g:link>
      
      ${remoteLink(update: '<caret>')}
      
      """);

    checkCompletionVariants(view, "ddd", "lll");
  }
}
