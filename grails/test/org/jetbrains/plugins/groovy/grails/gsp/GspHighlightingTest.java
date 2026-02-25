// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.groovy.grails.gsp;

import org.jetbrains.plugins.grails.fileType.GspFileType;
import org.jetbrains.plugins.groovy.lang.highlighting.GrHighlightingTestBase;

public class GspHighlightingTest extends GrHighlightingTestBase {
  public void testGroovyInsideGsp() {
    myFixture.configureByText(GspFileType.GSP_FILE_TYPE, """
      <%@ page contentType="text/html;charset=UTF-8"%>
      <html>
      <head>
          <title></title>
      </head>
      <body>
      <%
          def <info descr="null" textAttributesKey="Groovy var">a</info>
          def <info descr="null" textAttributesKey="Groovy var">x</info>
          def <info descr="null" textAttributesKey="Groovy var">c</info> = <info descr="null" textAttributesKey="Groovy var">a</info> + <info descr="null" textAttributesKey="Groovy var">x</info>
          if (true) {
              <info descr="null" textAttributesKey="Instance field">out</info>.println <info descr="null" textAttributesKey="Groovy var">c</info>
          }
      %>
      </body>
      </html>
      """);
    myFixture.testHighlighting(false, true, true);
  }
}
