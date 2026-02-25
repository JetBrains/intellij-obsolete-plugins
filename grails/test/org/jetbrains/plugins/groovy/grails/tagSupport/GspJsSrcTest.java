// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.groovy.grails.tagSupport;

import org.jetbrains.plugins.groovy.grails.GrailsTestCase;

public class GspJsSrcTest extends GrailsTestCase {
  public void testHighlighting() {
    myFixture.addFileToProject("web-app/js/site/forms.js", "");

    configureByView("a.gsp", """
      <g:javascript src="site/forms.js" />
      <g:javascript src="site/<error>aaa.js</error>" />
      <g:javascript src="<error>aaa.js</error>" />
      """);

    myFixture.checkHighlighting(true, false, true);
  }
}
