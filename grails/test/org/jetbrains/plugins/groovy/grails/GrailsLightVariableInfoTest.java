// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.groovy.grails;

import com.intellij.codeInsight.navigation.CtrlMouseHandler;
import com.intellij.psi.PsiReference;
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase;
import org.junit.Assert;

public class GrailsLightVariableInfoTest extends LightJavaCodeInsightFixtureTestCase {
  public void testInfo() {
    myFixture.configureByText("a.gsp", """      
      <g:each in="[1,2]" var="iii">
        ${i<caret>ii}
      </g:each>
      """);

    PsiReference ref = myFixture.getFile().findReferenceAt(myFixture.getEditor().getCaretModel().getOffset());
    Assert.assertTrue(CtrlMouseHandler.getInfo(ref.resolve(), ref.getElement()).contains("Integer"));
  }
}
