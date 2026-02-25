// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.groovy.grails.gsp;

import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.psi.PsiFile;
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase;
import com.intellij.util.containers.ContainerUtil;
import junit.framework.TestCase;
import org.jetbrains.plugins.grails.fileType.GspFileType;
import org.jetbrains.plugins.groovy.codeInspection.untypedUnresolvedAccess.GrUnresolvedAccessInspection;

public class GspCreateVariableQuickFixTest extends LightJavaCodeInsightFixtureTestCase {
  public void testCreateVariableAndCodeBlock() {
    PsiFile file = myFixture.configureByText(GspFileType.GSP_FILE_TYPE, """
      <div>
      \t${nonExistentVar<caret>}
      </div>
      """);
    myFixture.enableInspections(new GrUnresolvedAccessInspection());
    IntentionAction action = ContainerUtil.find(myFixture.getAvailableIntentions(),
                                                intention -> intention.getText().contains("Create variable"));
    TestCase.assertNotNull(action);
    myFixture.launchAction(action);

    TestCase.assertEquals("""
                            <%
                                def nonExistentVar
                            %>
                            <div>
                            \t${nonExistentVar}
                            </div>
                            """, file.getText());
  }

  public void testCreateVariable() {
    PsiFile file = myFixture.configureByText(GspFileType.GSP_FILE_TYPE, """
      <%
        def x = 12;
      %>
      <div>
      \t${nonExistentVar<caret>}
      </div>
      """);
    myFixture.enableInspections(new GrUnresolvedAccessInspection());
    IntentionAction action = ContainerUtil.find(myFixture.getAvailableIntentions(),
                                                intention -> intention.getText().contains("Create variable"));
    TestCase.assertNotNull(action);
    myFixture.launchAction(action);

    TestCase.assertEquals("""
                            <%
                              def x = 12;
                              def nonExistentVar
                            %>
                            <div>
                            \t${nonExistentVar}
                            </div>
                            """, file.getText());
  }
}
