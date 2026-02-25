// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.groovy.grails.gsp;

import com.intellij.lang.injection.InjectedLanguageManager;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.psi.ElementManipulator;
import com.intellij.psi.ElementManipulators;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.impl.source.tree.injected.InjectedLanguageUtil;
import com.intellij.testFramework.UsefulTestCase;
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase;
import junit.framework.TestCase;
import org.jetbrains.plugins.grails.lang.gsp.GspLanguage;
import org.jetbrains.plugins.grails.lang.gsp.psi.groovy.impl.GspOuterHtmlElementImpl;

public class GspElementManipulatorForOuterLanguageElementTest extends LightJavaCodeInsightFixtureTestCase {
  public void testContentChange() {
    PsiFile file = myFixture.configureByText("a.gsp", """
      <g:javascript>
          <caret>var a = 1
      </g:javascript>
      """);

    final PsiElement element = InjectedLanguageManager.getInstance(getProject()).getTopLevelFile(file).getViewProvider()
      .findElementAt(InjectedLanguageUtil.getTopLevelEditor(myFixture.getEditor()).getCaretModel().getOffset(), GspLanguage.INSTANCE);
    UsefulTestCase.assertInstanceOf(element, GspOuterHtmlElementImpl.class);

    final ElementManipulator<PsiElement> manipulator = ElementManipulators.getManipulator(element);
    TestCase.assertNotNull(manipulator);

    WriteCommandAction.runWriteCommandAction(getProject(), (Runnable)() -> manipulator.handleContentChange(element, "aaa"));

    myFixture.checkResult("""
                            <g:javascript>aaa</g:javascript>
                            """);
  }
}
