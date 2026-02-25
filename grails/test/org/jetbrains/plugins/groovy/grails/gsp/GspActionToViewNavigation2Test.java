// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.groovy.grails.gsp;

import com.intellij.psi.PsiFile;
import org.jetbrains.plugins.groovy.grails.Grails14TestCase;

public class GspActionToViewNavigation2Test extends Grails14TestCase {
  public void testGuttersExists() {
    myFixture.addFileToProject("grails-app/views/ccc/index.gsp", "");
    myFixture.addFileToProject("grails-app/views/ccc/xxx.gsp", "");
    myFixture.addFileToProject("grails-app/views/ccc/zzz.gsp", "");
    myFixture.addFileToProject("grails-app/views/ccc/notAAction.gsp", "");
    myFixture.addFileToProject("grails-app/views/ccc/notAAction2.gsp", "");

    PsiFile file = addController(
      """
        class CccController {
          def index = {
        
          }
        
          def xxx() {
          }
        
          def zzz(String param1, String param2) {
          }
        
          def notAAction;
        
          public static def getNotAAction2() {
            return 23
          }
        
        }
        """);
    checkGutters(file, "index", "xxx", "zzz");
  }
}
