// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.groovy.grails.gsp;

import com.intellij.codeInsight.lookup.LookupElement;
import junit.framework.TestCase;
import org.jetbrains.plugins.groovy.grails.GrailsTestCase;

public class GspCompletionEmptyTagTest extends GrailsTestCase {
  public void testCompletionEmptyTag() {
    addTaglib("""
                class MyTagLib {
                
                  def tagWithoutBody = {attr ->
                  }
                
                }
                """);
    configureByView("a.gsp", "<g:tagWithout<caret>");
    LookupElement[] res = myFixture.completeBasic();
    TestCase.assertNull(res);

    myFixture.checkResult("<g:tagWithoutBody/><caret>");
  }

  public void testEmptyTagJavadoc() {
    addTaglib("""
                class MyTagLib {
                
                  /**
                   * @emptyTag
                   */
                  def xxx = { attr, body ->
                
                  }
                }
                """);

    configureByView("a.gsp", "<g:xx<caret>");
    myFixture.completeBasic();

    myFixture.checkResult("<g:xxx/><caret>");
  }
}
