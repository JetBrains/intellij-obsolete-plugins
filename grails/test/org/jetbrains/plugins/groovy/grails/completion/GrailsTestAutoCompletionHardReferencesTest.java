// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.groovy.grails.completion;

import com.intellij.psi.PsiFile;
import com.intellij.testFramework.UsefulTestCase;
import com.intellij.testFramework.fixtures.CompletionAutoPopupTestCase;

public class GrailsTestAutoCompletionHardReferencesTest extends CompletionAutoPopupTestCase {
  public void testCompletion() {
    myFixture.addFileToProject("folder1/a.txt", "");
    myFixture.addFileToProject("folder2/a.txt", "");
    PsiFile file = myFixture.addFileToProject("grails-app/conf/BuildConfig.groovy", """
      grails.project.class.dir = "<caret>"\s""");

    myFixture.configureFromExistingVirtualFile(file.getVirtualFile());

    type("f");

    UsefulTestCase.assertSameElements(myFixture.getLookupElementStrings(), "folder1", "folder2");
  }
}
