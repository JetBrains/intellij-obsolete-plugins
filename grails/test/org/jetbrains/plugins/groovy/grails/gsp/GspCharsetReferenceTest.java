// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.groovy.grails.gsp;

import com.intellij.openapi.util.registry.Registry;
import com.intellij.psi.PsiFile;
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase;
import org.jetbrains.plugins.groovy.grails.GrailsTestCase;

public class GspCharsetReferenceTest extends LightJavaCodeInsightFixtureTestCase {
  public void testCharsetCompletion() {
    PsiFile file = myFixture.addFileToProject("a.gsp", """
      <%@ page contentType="text/html;charSet=<caret>" %>""");
    GrailsTestCase.checkCompletionStatic(myFixture, file, "UTF-8", "windows-1252");
  }

  public void testContentTypeCompletion() {
    Registry.get("ide.completion.variant.limit").setValue(10000, getTestRootDisposable());

    PsiFile file = myFixture.addFileToProject("a.gsp", """
      <%@ page contentType=" <caret>" %>""");
    GrailsTestCase.checkCompletionStatic(myFixture, file, "text/html", "application/activemessage", "image/jpeg");
  }
}
