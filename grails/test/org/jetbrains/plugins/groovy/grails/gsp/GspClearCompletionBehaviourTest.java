// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.groovy.grails.gsp;

import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import junit.framework.TestCase;
import org.jetbrains.plugins.groovy.grails.GrailsTestCase;

public class GspClearCompletionBehaviourTest extends GrailsTestCase {
  public void testClearCompletionBehaviour() {
    PsiFile fileA = myFixture.configureByText("a.gsp", "<g<caret>");
    myFixture.completeBasic();// Completion variants from GspCompletionContributor
    myFixture.type("lin\n");

    PsiDocumentManager.getInstance(getProject()).commitAllDocuments();

    PsiFile fileB = myFixture.configureByText("b.gsp", "<g:lin<caret>");
    myFixture.completeBasic();// Completion variants from LegacyCompletionContributor
    myFixture.type("\n");

    TestCase.assertEquals(fileA.getText(), fileB.getText());
  }
}
