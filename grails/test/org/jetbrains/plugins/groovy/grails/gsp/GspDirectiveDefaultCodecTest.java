// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.groovy.grails.gsp;

import org.jetbrains.plugins.groovy.grails.GrailsTestCase;

/**
 * @author sergey
 */
public class GspDirectiveDefaultCodecTest extends GrailsTestCase {
  public void testCompletion() {
    myFixture.addFileToProject("a.gsp", "<%@ page defaultCodec=\"SHA1<caret>\" %>");
    myFixture.testCompletionVariants("a.gsp", "SHA1", "SHA1Bytes");
  }
}
