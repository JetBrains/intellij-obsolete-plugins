// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.groovy.grails.completion;

import com.intellij.psi.PsiFile;
import org.jetbrains.plugins.groovy.grails.GrailsTestCase;

public class ServiceCompletionTest extends GrailsTestCase {
  public void testLogReference() {
    PsiFile service = myFixture.addFileToProject("grails-app/services/TestService.groovy", """
      class TestService {
        def x= {
           lo<caret>.error "Error";
        }
      }
      """);
    myFixture.configureFromExistingVirtualFile(service.getVirtualFile());
    myFixture.completeBasic();
    myFixture.assertPreferredCompletionItems(0, "log", "long");
  }
}
