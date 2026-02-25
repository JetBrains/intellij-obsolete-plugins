// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.groovy.grails.gsp;

import com.intellij.psi.PsiFile;
import org.jetbrains.plugins.groovy.GroovyLanguage;
import org.jetbrains.plugins.groovy.grails.GrailsTestCase;

public class GspPropertyResolvePriorityTest extends GrailsTestCase {
  /**
   * Test: g:each Variables > ModelVariables > DefaultVariables, Taglibs
   */
  public void testResolvePriority1() {
    addController(
      """
        class CccController {
          def index = {
            [exception:"string"]
          }
        }
        """);
    addTaglib(
      """
        class MyTagLib {
          def exception = {
            out << "exception"
          }
        }
        """);
    addTaglib(
      """
        class MyWithNamespaceTagLib {
          static namespace='exception'
          def exception = {
            out << "exception"
          }
        }
        """);
    PsiFile gspFile = myFixture.addFileToProject("grails-app/views/ccc/index.gsp", """
      <% out << exception.substring(1) %>
      
      <g:each var="exception" in="[1,2,3]">
        ${exception.byteValue()}<br/>
      </g:each>
      """);
    GrailsTestCase.checkResolve(gspFile.getViewProvider().getPsi(GroovyLanguage.INSTANCE));
  }
}
