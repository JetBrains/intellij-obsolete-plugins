// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.groovy.grails.gsp;

import org.jetbrains.plugins.groovy.grails.GrailsTestCase;

public class GspSdkTagCompletionTest extends GrailsTestCase {
  @Override
  protected boolean useGrails14() {
    return true;
  }

  @Override
  protected boolean includeSources() {
    return true;
  }

  public void testCompletionEmptyTagWithAttr() {
    configureByView("a.gsp", "<g:messag<caret>");
    myFixture.completeBasic();
    myFixture.checkResult("<g:message<caret>");
  }
}
