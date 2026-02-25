// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.groovy.grails.gsp;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.psi.PsiFile;
import junit.framework.TestCase;
import org.jetbrains.plugins.groovy.grails.GrailsTestCase;

public class GspLayoutTagTest extends GrailsTestCase {
  public void testRename() {
    PsiFile layout = myFixture.addFileToProject("grails-app/views/layouts/main.gsp", "");
    PsiFile page = myFixture.addFileToProject("grails-app/views/ccc/page.gsp", "<meta name='layout' content='main'>");

    myFixture.renameElement(layout, "lll");
    TestCase.assertEquals("<meta name='layout' content='lll'>", page.getText());
  }

  public void testMove() {
    myFixture.addFileToProject("grails-app/views/layouts/ccc/main.gsp", "");
    PsiFile page = myFixture.addFileToProject("grails-app/views/ccc/page.gsp", "<meta name='layout' content='ccc/main'>");

    myFixture.moveFile("grails-app/views/layouts/ccc/main.gsp", "grails-app/views/layouts");
    TestCase.assertEquals("<meta name='layout' content='main'>", page.getText());
  }

  public void testMakeNonLayout() {
    myFixture.addFileToProject("grails-app/views/layouts/ccc/main.gsp", "");
    PsiFile page = myFixture.addFileToProject("grails-app/views/ccc/page.gsp", "<meta name='layout' content='ccc/main'>");

    myFixture.moveFile("grails-app/views/layouts/ccc/main.gsp", "grails-app/views");
    TestCase.assertEquals("<meta name='layout' content='ccc/main'>", page.getText());
  }

  public void testCompletion1() {
    myFixture.addFileToProject("grails-app/views/layouts/ccc/main.gsp", "");
    PsiFile page = myFixture.addFileToProject("grails-app/views/ccc/page.gsp", "<meta name='layout' content='ccc/mai<caret>'>");

    myFixture.configureFromExistingVirtualFile(page.getVirtualFile());
    LookupElement[] res = myFixture.completeBasic();
    TestCase.assertNull(res);

    TestCase.assertEquals("<meta name='layout' content='ccc/main'>", page.getText());
  }

  public void testCompletion2() {
    myFixture.addFileToProject("grails-app/views/layouts/main.gsp", "");
    myFixture.addFileToProject("grails-app/views/layouts/lll.gsp", "");
    PsiFile file = myFixture.addFileToProject("grails-app/views/ccc/page.gsp", "<meta name='layout' content='<caret>'>");
    checkCompletionVariants(file, "lll", "main");
  }

  public void testHighlighting() {
    myFixture.addFileToProject("grails-app/views/layouts/ccc/main.gsp", "");
    PsiFile file = myFixture.addFileToProject("grails-app/views/ccc/page.gsp", """
      <meta name='layout' content='<warning descr="Cannot resolve file 'main'">main</warning>'>
      <meta name='layout' content='<warning descr="Cannot resolve file 'main.gsp'">main.gsp</warning>'>
      <meta name='layout' content='ccc/main'>
      <meta name='layout' content='ccc/main.gsp'>
      <meta name='layout' content='/ccc/main.gsp'>
      <meta name='layout' content='ccc/main.gsp/<warning descr="Cannot resolve file ''"></warning>'>
      <meta name='author' content='main'>
      <meta content='main'>
      """);

    myFixture.testHighlighting(true, false, true, file.getVirtualFile());
  }

  public void testApplyLayoutCompletion() {
    myFixture.addFileToProject("grails-app/views/layouts/main.gsp", "");
    myFixture.addFileToProject("grails-app/views/layouts/lll.gsp", "");
    PsiFile file = myFixture.addFileToProject("grails-app/views/ccc/page.gsp", "<g:applyLayout name=\"<caret>\"> </g:applyLayout>");
    checkCompletionVariants(file, "lll", "main");
  }
}
