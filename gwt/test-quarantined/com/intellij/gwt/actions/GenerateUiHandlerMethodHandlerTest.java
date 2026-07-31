package com.intellij.gwt.actions;

import com.intellij.codeInsight.generation.ClassMember;
import com.intellij.gwt.references.GwtCodeInsightTestCase;
import com.intellij.openapi.project.Project;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.impl.source.PostprocessReformattingAspect;

public class GenerateUiHandlerMethodHandlerTest extends GwtCodeInsightTestCase {

  public void testCombined() {
    doTest(false, "MyComponent_combined.java");
  }

  public void testSeparated() {
    doTest(true, "MyComponent_separated.java");
  }

  private void doTest(boolean separate, String fileName) {
    myCodeInsightFixture.configureByFiles("MyComponent.java", "MyComponent.ui.xml");
    Project project = myCodeInsightFixture.getProject();

    PsiClass clickEventClass = JavaPsiFacade.getInstance(project).findClass("com.google.gwt.event.dom.client.ClickEvent",
                                                                            myCodeInsightFixture.getModule().getModuleWithLibrariesScope());
    assertNotNull(clickEventClass);

    new GenerateUiHandlerMethodHandler() {
      {
        this.myGenerateSeparateMethods = separate;
      }

      @Override
      protected ClassMember[] chooseOriginalMembers(PsiClass aClass, Project project) {
        ClassMember[] members = getAllOriginalMembers(aClass);
        for (ClassMember member : members) {
          ((UiFieldClassMember)member).setEventClass(clickEventClass);
        }
        return members;
      }
    }.invoke(project, myCodeInsightFixture.getEditor(), myCodeInsightFixture.getFile());

    PostprocessReformattingAspect.getInstance(project).doPostponedFormatting();

    myCodeInsightFixture.checkResultByFile(fileName);
  }

  @Override
  protected String getBaseDirectoryPath() {
    return "/actions/createUiHandlerMethodHandler/";
  }
}
