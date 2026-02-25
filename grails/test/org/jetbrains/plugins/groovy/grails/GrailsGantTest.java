// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.groovy.grails;

import com.intellij.psi.PsiFile;

public class GrailsGantTest extends GrailsTestCase {
  public void testResolve() {
    PsiFile script = myFixture.addFileToProject("scripts/SomeScript.groovy", """
      target(main: '''Script used to interact with remote Tomcat. The following subcommands are available:
      grails tomcat deploy - Deploy to a tomcat server
      grails tomcat undeploy - Undeploy from a tomcat server
      ''') {
          depends("aaa", "bbb", "ccc")
          // Do somthing
        }
      }
      setDefaultTarget("main")
      """);

    GrailsTestCase.checkResolve(script);
  }
}
