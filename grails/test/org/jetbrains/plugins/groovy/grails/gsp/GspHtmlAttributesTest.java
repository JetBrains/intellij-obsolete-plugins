// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.groovy.grails.gsp;

import com.intellij.jsp.impl.TldDescriptor;
import com.intellij.psi.PsiFile;
import com.intellij.psi.xml.XmlDocument;
import com.intellij.testFramework.UsefulTestCase;
import com.intellij.xml.XmlElementDescriptor;
import org.jetbrains.plugins.grails.lang.gsp.psi.gsp.impl.gtag.GspTagDescriptorService;
import org.jetbrains.plugins.groovy.grails.GrailsTestCase;

import java.util.Set;
import java.util.TreeSet;

public class GspHtmlAttributesTest extends GrailsTestCase {
  @Override
  protected boolean useGrails14() {
    return true;
  }

  /**
   * Check that all SDK tags are present in GspHtmlAttributeCache.tagMap.
   * If new SDK tags will added on grails release this test will fail.
   */
  public void testAllTagsInMap() {
    TldDescriptor tldDescriptor = GspTagDescriptorService.getTldDescriptor(getProject());

    PsiFile gspFile = myFixture.addFileToProject("a.gsp", "");

    XmlDocument document = (XmlDocument)gspFile.getFirstChild();

    Set<String> tagNames = new TreeSet<>();

    for (XmlElementDescriptor d : tldDescriptor.getRootElementsDescriptors(document)) {
      tagNames.add(d.getName());
    }


    for (XmlElementDescriptor d : document.getRootTag().getDescriptor().getElementsDescriptors(null)) {
      tagNames.add(d.getName());
    }


    tagNames.removeAll(GspTagDescriptorService.getAllTags());
    UsefulTestCase.assertEmpty(tagNames);
  }

  public void testCompletion() {
    configureByView("a.gsp", "<g:link onmouse<caret> />");
    checkCompletion("onmousedown", "onmousemove", "onmouseout", "onmouseover", "onmouseup");
  }
}
