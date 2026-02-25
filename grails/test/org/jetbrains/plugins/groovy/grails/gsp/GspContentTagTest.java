// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.groovy.grails.gsp;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInspection.htmlInspections.HtmlUnknownAttributeInspection;
import com.intellij.codeInspection.htmlInspections.HtmlUnknownTagInspection;
import com.intellij.psi.PsiFile;
import org.jetbrains.plugins.groovy.grails.GrailsTestCase;
import org.junit.Assert;

public class GspContentTagTest extends GrailsTestCase {
  public void testHighlighting() {
    myFixture.enableInspections(HtmlUnknownTagInspection.class, HtmlUnknownAttributeInspection.class);

    PsiFile file = myFixture.addFileToProject("a.gsp", """
      
      <content tag="" <warning descr="Attribute fff is not allowed here">fff</warning>="">
        <div>
          Abc
        </div>
      </content>
      <<warning descr="Unknown html tag fgdfgdfgkdflgdf">fgdfgdfgkdflgdf</warning>>
        Abc
      </<warning descr="Unknown html tag fgdfgdfgkdflgdf">fgdfgdfgkdflgdf</warning>>
      """);
    myFixture.testHighlighting(true, false, true, file.getVirtualFile());
  }

  public void testCompletionAttribute() {
    PsiFile file = myFixture.configureByText("a.gsp", """
      
      <content t<caret>
      </content>
      """);
    LookupElement[] res = myFixture.completeBasic();
    Assert.assertNull(res);

    Assert.assertEquals("""
                            
                            <content tag=""
                            </content>
                            """, file.getText());
  }

  public void testCompletionContent() {
    myFixture.configureFromExistingVirtualFile(myFixture.addFileToProject("a.gsp", """
      
      <content tag="">
        <inpu<caret>
      </content>
      """).getVirtualFile());
    myFixture.completeBasic();
    myFixture.assertPreferredCompletionItems(0, "input");
  }
}
