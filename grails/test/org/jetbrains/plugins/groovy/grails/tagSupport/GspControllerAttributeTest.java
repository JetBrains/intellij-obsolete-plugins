// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.groovy.grails.tagSupport;

import com.intellij.codeInsight.completion.CompletionType;
import com.intellij.psi.PsiFile;
import junit.framework.TestCase;
import org.jetbrains.plugins.groovy.grails.GrailsTestCase;

import java.util.List;

public class GspControllerAttributeTest extends GrailsTestCase {
  public void testActionCompletion() {
    addController("class C1Controller { def actionC1={}}");
    addController("class C2Controller { def actionC2={}}");
    addController("class C3Controller { def actionC3={}}");

    configureByView("test/page.gsp", "<g:link controller='<caret>'/>");

    myFixture.complete(CompletionType.BASIC);

    TestCase.assertEquals(List.of("c1", "c2", "c3"), myFixture.getLookupElementStrings());
  }

  public void testFieldActionRename() {
    addController("class C1Controller { def actionC1={}}");
    PsiFile controllerTest1 = myFixture.addFileToProject("test/unit/C1ControllerTest.groovy", "class C1ControllerTest { }");
    PsiFile controllerTest2 = myFixture.addFileToProject("test/unit/C1ControllerTests.groovy", "class C1ControllerTests { }");

    PsiFile file = myFixture.addFileToProject("grails-app/views/c1/page.gsp", "<g:link controller='c1<caret>'/>");

    myFixture.configureFromExistingVirtualFile(file.getVirtualFile());
    myFixture.renameElementAtCaret("CccController");

    TestCase.assertEquals("<g:link controller='ccc'/>", file.getText());
    TestCase.assertEquals("ccc", file.getParent().getName());
    TestCase.assertEquals("CccControllerTest.groovy", controllerTest1.getName());
    TestCase.assertEquals("CccControllerTests.groovy", controllerTest2.getName());
  }

  public void testMakeNotController() {
    addController("class C1Controller { def actionC1={}}");

    PsiFile file = myFixture.addFileToProject("grails-app/views/test/page.gsp", "<g:link controller='c1<caret>'");

    myFixture.configureFromExistingVirtualFile(file.getVirtualFile());
    myFixture.renameElementAtCaret("Ccc");

    TestCase.assertEquals("<g:link controller='c1'", file.getText());
  }

  public void testDecapitalizeControllerName() {// All letters in controller name are upper case.
    addController("class CCCController { def actionC1={}}");

    PsiFile file = myFixture.addFileToProject("grails-app/views/test/page.gsp", "<g:link controller='C<caret>'/>");

    myFixture.configureFromExistingVirtualFile(file.getVirtualFile());

    myFixture.complete(CompletionType.BASIC);

    TestCase.assertEquals("<g:link controller='CCC'/>", file.getText());

    myFixture.renameElementAtCaret("RRRController");

    TestCase.assertEquals("<g:link controller='RRR'/>", file.getText());
  }
}
