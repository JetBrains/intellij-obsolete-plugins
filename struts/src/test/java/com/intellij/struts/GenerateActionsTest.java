/*
 * Copyright 2000-2007 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.intellij.struts;

import com.intellij.codeInsight.actions.CodeInsightAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.struts.inplace.generate.GenerateActionMappingAction;
import com.intellij.struts.inplace.generate.GenerateFormAction;
import com.intellij.struts.inplace.generate.GenerateForwardAction;
import com.intellij.struts.inplace.generate.GenerateGlobalForwardAction;
import com.intellij.testFramework.builders.WebModuleFixtureBuilder;

/**
 * @author Dmitry Avdeev
 */
public class GenerateActionsTest extends StrutsTest {

  @Override
  protected void configure(WebModuleFixtureBuilder moduleBuilder) {
    moduleBuilder.addContentRoot(getTestDataPath());
    moduleBuilder.addSourceRoot("fakeStruts");
  }
  
  public void testGenerateAction() {
    doTest("/templates/action.xml", "/templates/action_after.xml", new GenerateActionMappingAction());
  }

  public void testGenerateForm() {
    doTest("/templates/action.xml", "/templates/form_after.xml", new GenerateFormAction());
  }

  public void testGenerateForward() {
    doTest("/templates/action.xml", "/templates/forward_after.xml", new GenerateGlobalForwardAction());
  }

  public void testGenerateLocalForward() {
    doTest("/templates/action_after.xml", "/templates/local-forward_after.xml", new GenerateForwardAction());
  }

  private void doTest(final String file, final String expectedFile, final CodeInsightAction action) {
    myFixture.configureByFile(file);
    final Editor editor = myFixture.getEditor();
    final Project project = myFixture.getProject();
    action.actionPerformedImpl(project, editor);
//    final TemplateState templateState = TemplateManagerImpl.getTemplateState(editor);
//    templateState.nextTab();
    myFixture.checkResultByFile(expectedFile);
  }
}
