// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.groovy.grails.gsp;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.testFramework.UsefulTestCase;
import org.jetbrains.plugins.groovy.grails.GrailsTestCase;
import org.junit.Assert;

public class GspCompletionInNonexistingNamespaceTest extends GrailsTestCase {
  public void testCompletionInNonexistingNamespace() {
    configureByView("a.gsp", "<fff:<caret>");
    LookupElement[] res = myFixture.completeBasic();
    Assert.assertNotNull(res);
    UsefulTestCase.assertEmpty(res);
  }
}
