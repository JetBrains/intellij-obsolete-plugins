// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.groovy.grails.tagSupport;

import junit.framework.TestCase;
import org.jetbrains.plugins.groovy.grails.GrailsTestCase;

import java.util.List;

public class GspCodecAttributeTest extends GrailsTestCase {
  public void testCodecHighlighting() {
    myFixture.addFileToProject("grails-app/utils/MmmCodec.groovy", """
      class MmmCodec {
       public static decode(Object target) { return "s"; }
      }
      """);

    myFixture.addFileToProject("grails-app/utils/NnnCodec.groovy", """
      class NnnCodec {
       public static encode(Object target) { return "s"; }
       public static decode(Object target) { return "s"; }
      }
      """);

    myFixture.addFileToProject("grails-app/views/a.gsp", """
      <g:message encodeAs="<error descr="Cannot resolve symbol 'Mmm'">Mmm</error>" />
      <g:message encodeAs="HTML" />
      <g:message encodeAs="SHA1" />
      <g:message encodeAs="Nnn" />
      """);

    myFixture.addFileToProject("grails-app/views/b.gsp", "<g:message encodeAs='<caret>' />");

    List<String> v = myFixture.getCompletionVariants("grails-app/views/b.gsp");
    TestCase.assertTrue(v.contains("Nnn"));
    TestCase.assertFalse(v.contains("Mmm"));
  }
}
