package com.intellij.gwt.intentions;

import com.intellij.gwt.uiBinder.CreateUiHandlerIntention;
import com.intellij.openapi.project.Project;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiType;
import com.intellij.psi.search.GlobalSearchScope;

public class CreateUiHandlerIntentionTest extends GwtIntentionTestBase {

  public void testNoMethods() {
    doTest("MyComponentWithoutMethods.ui.xml", "MyComponentWithoutMethods.java", "MyComponentWithoutMethods_after.java", createIntention());
  }

  public void testOneMethod() {
    doTest("MyComponentWithFirstMethod.ui.xml", "MyComponentWithFirstMethod.java", "MyComponentWithFirstMethod_after.java", createIntention());
  }

  public void testMethodAlreadyExist() {
    doTest("MyComponentWithBothMethods.ui.xml", "MyComponentWithBothMethods.java", "MyComponentWithBothMethods.java", createIntention());
  }

  private static CreateUiHandlerIntention createIntention() {
    return new CreateUiHandlerIntention() {
      @Override
      protected PsiClass getEventClass(Project project, GlobalSearchScope resolveScope, PsiType fieldType) {
        return JavaPsiFacade.getInstance(project).findClass("com.google.gwt.event.dom.client.ClickEvent", resolveScope);
      }
    };
  }

  @Override
  protected String getBaseDirectoryPath() {
    return "intentions/createUiHandler";
  }
}
