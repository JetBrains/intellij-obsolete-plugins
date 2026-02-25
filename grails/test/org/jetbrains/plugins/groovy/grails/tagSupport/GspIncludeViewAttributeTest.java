// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.groovy.grails.tagSupport;

import org.jetbrains.plugins.groovy.grails.GrailsTestCase;

public class GspIncludeViewAttributeTest extends GrailsTestCase {
  public void testHighlighting() {
    addView("ccc/a.gsp", "");

    configureByView("ccc/test.gsp", """
      <g:include view="/ccc/a.gsp" />
      <g:include view="ccc/a.gsp" />
      
      <g:include view="<error>a.gsp</error>" />
      <g:include view="ccc/a.gsp/<error></error>" />
      <g:include view="/<error>a.gsp</error>" />
      <g:include view="<error>a</error>" />
      <g:include view="/ccc/<error>a</error>" />
      """);

    myFixture.checkHighlighting(true, false, true);
  }

  public void testRename() {
    addView("ccc/a.gsp", "");

    configureByView("ccc/test.gsp", """
      <g:include view="/ccc/a.gsp" />
      <g:include view="ccc/a.gsp<caret>" />
      ${ include(view: ""\"ccc/a.gsp""\") }
      ${ include(view: '/ccc/a.gsp') }
      """);

    myFixture.renameElementAtCaret("zzz.gsp");

    myFixture.checkResult("""
                            <g:include view="/ccc/zzz.gsp" />
                            <g:include view="ccc/zzz.gsp" />
                            ${ include(view: ""\"ccc/zzz.gsp""\") }
                            ${ include(view: '/ccc/zzz.gsp') }
                            """);
  }
}
