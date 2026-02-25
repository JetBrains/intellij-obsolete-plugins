// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.groovy.grails.gsp;

import com.intellij.psi.PsiFile;
import org.jetbrains.plugins.groovy.grails.GrailsTestCase;

public class GspActionToViewNavigationTest extends GrailsTestCase {
  public void testGuttersExists() {
    myFixture.addFileToProject("grails-app/views/ccc/index.gsp", "");
    myFixture.addFileToProject("grails-app/views/ccc/xxx.gsp", "");
    myFixture.addFileToProject("grails-app/views/ccc/zzz.jsp", "");
    myFixture.addFileToProject("grails-app/views/ccc/notAAction.gsp", "");
    myFixture.addFileToProject("grails-app/views/ccc/notAAction2.gsp", "");

    PsiFile file = addController(
      """
        class CccController {
          def index = {
        
          }
        
          def notAAction = 100;
        
          def xxx = null;
        
          public def zzz = {
        
          }
        
          static notAAction2 = {}
        
          def actionWithoutGsp = {}
        
        }
        """);
    checkGutters(file, "index", "zzz");
  }
}
