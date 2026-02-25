// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.groovy.grails.gsp;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.jetbrains.plugins.groovy.grails.GrailsTestCase;
import org.junit.Assert;

public class GspJavascriptTest extends GrailsTestCase {
  public void testCompletionInGspTag() {
    PsiFile file = addView("a.gsp", """
      <html>
      <script>
        var xxx1 = 1;
      </script>
      <g:javascript>
        var xxx2 = 2
      </g:javascript>
      
      <g:javascript>
        xxx<caret>
      </g:javascript>
      </html>
      """);

    checkCompletionVariants(file, "xxx1", "xxx2");
  }

  public void testCompletionInGspAttribute() {
    PsiFile file = addView("a.gsp", """
      <html>
      
      <g:link onclick="xxx<caret>">link</g:link>
      
      <script>
        var xxx1 = 1;
      
        function xxx3() {}
      </script>
      <g:javascript>
        var xxx2 = 2
      
      </g:javascript>
      
      </html>
      """);

    checkCompletionVariants(file, "xxx1", "xxx2", "xxx3");
  }

  public void testCompletionInGspTagFunction() {
    PsiFile file = addView("a.gsp", """
      <html>
      <script>
        function xxx1() {}
      </script>
      <g:javascript>
        function xxx2() {}
      </g:javascript>
      
      <g:javascript>
        xxx<caret>
      </g:javascript>
      </html>
      """);

    checkCompletionVariants(file, "xxx1", "xxx2");
  }

  public void testCompletionInHtmlTag() {
    PsiFile file = addView("a.gsp", """
      <html>
      <script>
        var xxx1 = 1;
      </script>
      <g:javascript>
        var xxx2 = 2
      </g:javascript>
      
      <script>
        xxx<caret>
      </script>
      </html>
      """);

    checkCompletionVariants(file, "xxx1", "xxx2");
  }

  public void testCompletionInHtmlTagFunction() {
    PsiFile file = addView("a.gsp", """
      <html>
      <script>
        function xxx1() {}
      </script>
      <g:javascript>
        function xxx2() {}
      </g:javascript>
      
      <script>
        xxx<caret>
      </script>
      </html>
      """);

    checkCompletionVariants(file, "xxx1", "xxx2");
  }

  public void testCompletionInTemplate() {
    addController("class CccController {}");

    addView("ccc/a.gsp", """
      <script>
        var xxx1 = 1
      </script>
      <g:javascript>
        var xxx2 = 2
      </g:javascript>
      
      <tmpl:ttt/>
      """);

    PsiFile file = addView("ccc/_ttt.gsp", """
      <script>
      var x = xxx<caret>
      </script>
      """);

    checkCompletionVariants(file, "xxx1", "xxx2");
  }

  public void testCompletionInTemplateFunction() {
    addController("class CccController {}");

    addView("ccc/a.gsp", """
      <script>
        function xxx1() {}
      </script>
      <g:javascript>
        function xxx2() {}
      </g:javascript>
      
      <tmpl:ttt/>
      """);

    PsiFile file = addView("ccc/_ttt.gsp", """
      <script>
      var x = xxx<caret>
      </script>
      """);

    checkCompletionVariants(file, "xxx1", "xxx2");
  }

  public void testResolve() {
    addController("class CccController {}");

    addView("ccc/a.gsp", """
      <script>
        var xxx1 = 1
      </script>
      <tmpl:ttt/>
      """);

    configureByView("ccc/_ttt.gsp", """
      <script>
      var x = xxx1<caret>
      </script>
      """);

    PsiElement x = myFixture.getElementAtCaret();

    Assert.assertEquals("a.gsp", x.getContainingFile().getName());
  }

  public void testResolveFunction() {
    addController("class CccController {}");

    addView("ccc/a.gsp", """
      <script>
        function xxx1() {}
      </script>
      <tmpl:ttt/>
      """);

    configureByView("ccc/_ttt.gsp", """
      <script>
      var x = xxx1<caret>()
      </script>
      """);

    PsiElement x = myFixture.getElementAtCaret();

    Assert.assertEquals("a.gsp", x.getContainingFile().getName());
  }
}
