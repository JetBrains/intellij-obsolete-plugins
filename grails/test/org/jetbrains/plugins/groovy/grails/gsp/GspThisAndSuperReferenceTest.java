// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.groovy.grails.gsp;

import com.intellij.psi.PsiFile;
import org.jetbrains.plugins.groovy.grails.GrailsTestCase;

public class GspThisAndSuperReferenceTest extends GrailsTestCase {
  public void testThis() {
    PsiFile gsp = myFixture.addFileToProject("a.gsp", "${this.<caret>}");
    GrailsTestCase.checkCompletionStatic(myFixture, gsp, "cleanup", "registerSitemeshPreprocessMode");
  }

  public void testSuper() {
    PsiFile gsp = myFixture.addFileToProject("a.gsp", "${super.<caret>}");
    GrailsTestCase.checkCompletionStatic(myFixture, gsp, "cleanup", "registerSitemeshPreprocessMode");
  }
}
