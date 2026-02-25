// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.groovy.grails.action;

import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.psi.PsiFile;
import junit.framework.TestCase;
import org.intellij.lang.annotations.Language;
import org.jetbrains.plugins.grails.GrailsBundle;
import org.jetbrains.plugins.groovy.grails.HddGrailsTestCase;

public class GenerateViewByActionTest extends HddGrailsTestCase {
  private void testAction(@Language("devkit-action-id") String actionId, boolean enabled) {
    AnAction action = ActionManager.getInstance().getAction(actionId);
    Presentation presentation = myFixture.testAction(action);
    TestCase.assertEquals(enabled, presentation.isEnabled());
    TestCase.assertEquals(enabled, presentation.isVisible());
  }

  public void testGenerateView1() {
    PsiFile controllerFile = myFixture.addFileToProject("grails-app/controllers/CccController.groovy", """
      class CccController {
        def index = {
          <caret>
        }
      }
      """);
    myFixture.configureFromExistingVirtualFile(controllerFile.getVirtualFile());
    testAction("Generate.GrailsView", true);
    TestCase.assertNotNull(myFixture.findFileInTempDir("grails-app/views/ccc/index.gsp"));
  }

  public void testViewAlreadyExists() {
    PsiFile controllerFile = myFixture.addFileToProject("grails-app/controllers/CccController.groovy", """
      class CccController {
        def index = {
          <caret>
        }
      }
      """);
    myFixture.addFileToProject("grails-app/views/ccc/index.gsp", "");
    myFixture.configureFromExistingVirtualFile(controllerFile.getVirtualFile());
    testAction("Generate.GrailsView", false);
  }

  public void testNotAnAction1() {
    PsiFile controllerFile = myFixture.addFileToProject("grails-app/controllers/CccController.groovy", """
      class CccController {
        def index = {
          return new Runnable() {
            def zzz = {
              <caret>
            }
          }
        }
      }
      """);
    myFixture.addFileToProject("grails-app/views/ccc/index.gsp", "");
    myFixture.configureFromExistingVirtualFile(controllerFile.getVirtualFile());
    testAction("Generate.GrailsView", false);
  }

  public void testNotAnAction2() {
    PsiFile controllerFile = myFixture.addFileToProject("grails-app/controllers/CccController.groovy", """
      class CccController {
        public int getZzz() {
          <caret>
        }
      }
      """);

    myFixture.configureFromExistingVirtualFile(controllerFile.getVirtualFile());

    testAction("Generate.GrailsView", false);
  }

  public void testIntention2() {
    PsiFile controllerFile = myFixture.addFileToProject("grails-app/controllers/CccController.groovy", """
      class CccController {
        def index<caret> = {
      
        }
      }
      """);

    runIntention(controllerFile, GrailsBundle.message("intention.text.create.view.gsp.page"), true);
    TestCase.assertNotNull(myFixture.findFileInTempDir("grails-app/views/ccc/index.gsp"));
  }
}
