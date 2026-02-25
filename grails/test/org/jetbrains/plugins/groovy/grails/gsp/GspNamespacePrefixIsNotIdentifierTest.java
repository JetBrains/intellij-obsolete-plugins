// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.groovy.grails.gsp;

import com.intellij.psi.PsiFile;
import org.jetbrains.plugins.groovy.grails.GrailsTestCase;

public class GspNamespacePrefixIsNotIdentifierTest extends GrailsTestCase {
  public void testNamespacePrefixIsNotIdentifier() {
    addTaglib(
      """
        class MyTagLib {
        
          static namespace = "import"
        
          def xxx = { }
          def yyy = { }
        
        }
        """);

    PsiFile b = myFixture.addFileToProject("grails-app/views/b.gsp", "<import:<caret>");
    checkCompletionVariants(b, "xxx", "yyy");
  }

  public void testAccessViaThis() {
    addTaglib(
      """
        class MyTagLib {
        
          static namespace = "import"
        
          def xxx = { }
          def yyy = { }
        
        }
        """);

    PsiFile gsp = addView("a.gsp", "${this.'import'.<caret>}");
    checkCompletion(gsp, "xxx", "yyy");
  }
}
