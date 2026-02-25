// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.groovy.grails.gsp;

import com.intellij.psi.PsiFile;
import org.jetbrains.plugins.groovy.grails.GrailsTestCase;

public class GspAttributeFromJavadocTest extends GrailsTestCase {
  public void testCompletion() {
    addTaglib("""
                class MyTagLib {
                
                  /**
                   * @attr aaa sdsda
                   * @attr bbb sdsda
                   */
                  def xxx = { attr ->
                    out << attr.xxx + attr.aaa
                  }
                }
                """);

    PsiFile file = addView("a.gsp", "<g:xxx <caret> />");
    myFixture.testCompletionVariants(getFilePath(file), "xxx", "aaa", "bbb");
  }

  public void testRequiredAttribute() {
    addTaglib("""
                class MyTagLib {
                
                  /**
                   * @attr aaa required sdsda
                   * @attr bbb REQUIRED sdsda
                   * @attr ccc
                   */
                  def xxx = { attr, body ->
                    out << body << attr
                  }
                }
                """);

    configureByView("a.gsp", "<g:xx<caret>");
    myFixture.completeBasic();
    myFixture.checkResult("<g:xxx aaa=\"\" bbb=\"\"");
  }
}
