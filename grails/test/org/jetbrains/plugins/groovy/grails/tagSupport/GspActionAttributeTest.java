// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.groovy.grails.tagSupport;

import com.intellij.psi.PsiFile;
import junit.framework.TestCase;
import org.jetbrains.plugins.groovy.grails.GrailsTestCase;

import static org.jetbrains.plugins.groovy.grails.GrailsTestUtil.getTestRootPath;

public class GspActionAttributeTest extends GrailsTestCase {

  @Override
  protected String getTestDataPath() {
    return getTestRootPath("/testdata/grails/gsp/");
  }

  public void testActionCompletion() {
    myFixture.copyFileToProject("TestController.groovy", "grails-app/controllers/TestController.groovy");

    addGroovyClass("grails-app/controllers", "package xxx; class Parent { def actionFromParent={}}");
    addController("package xxx; class TestController extends Parent { def namespace='NAMESPACE_MUST_BE_STATIC' def action10={}}");

    PsiFile file = addView("test/page.gsp", "<g:link action='<caret>'/>");

    checkCompletionVariants(file, "action10", "action4", "action5", "action6", "action7", "action8", "action9", "actionFromParent");
  }

  public void testFieldActionRename() {
    addController("class TestController { def action={}}");

    PsiFile file = myFixture.addFileToProject("grails-app/views/test/action.gsp",
                                              "<g:link action='action<caret>' /> <% link(action: 'action'); g.link(action: 'action') %>");
    PsiFile jspFile = myFixture.addFileToProject("grails-app/views/test/action.jsp", "");

    myFixture.configureFromExistingVirtualFile(file.getVirtualFile());
    myFixture.renameElementAtCaret("newName");

    TestCase.assertEquals("<g:link action='newName' /> <% link(action: 'newName'); g.link(action: 'newName') %>", file.getText());
    TestCase.assertEquals("newName.gsp", file.getName());
    TestCase.assertEquals("action.jsp", jspFile.getName());
  }

  public void testActionWithoutControllerInController() {
    configureByController("""
                            class CccController {
                              def index = {
                                render link(action: '<caret>', "Link Text") as String
                              }
                            }
                            """);

    myFixture.completeBasic();
    myFixture.type("i\t");// Test for #IDEA-64293
    myFixture.checkResult("""
                            class CccController {
                              def index = {
                                render link(action: 'index', "Link Text") as String
                              }
                            }
                            """);
  }
}
