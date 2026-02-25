// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.groovy.grails.parser;

import com.intellij.lang.Language;
import com.intellij.psi.PsiFile;
import com.intellij.psi.impl.DebugUtil;
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase;
import org.jetbrains.plugins.groovy.util.TestUtils;

public abstract class GspParsingTestCase extends LightJavaCodeInsightFixtureTestCase {

  protected void doTest(Language lang) {
    final String path = getTestName(true).replace('$', '/') + ".test";
    final String input = TestUtils.readInput(getTestDataPath() + path).get(0);

    final PsiFile file = TestUtils.createPseudoPhysicalFile(getProject(), "temp.gsp", input);
    final PsiFile psi = file.getViewProvider().getPsi(lang);
    final String actualPsiText = DebugUtil.psiToString(psi, true).trim();

    myFixture.configureByText("test.txt", input + "\n-----\n" + actualPsiText);
    myFixture.checkResultByFile(path, false);
  }
}
