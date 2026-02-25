// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.groovy.grails;

import com.intellij.psi.PsiFile;
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase;

public class GrailsResolveInConfigFileTest extends LightJavaCodeInsightFixtureTestCase {
  public void testBuildConfig() {
    PsiFile file = myFixture.addFileToProject("grails-app/conf/BuildConfig.groovy", """
      a.b.c1=appName
      a.b.c2=grailsHome
      """);

    GrailsTestCase.checkResolve(file, "a", "a");
  }

  public void testConfig() {
    PsiFile file = myFixture.addFileToProject("grails-app/conf/Config.groovy", """
      a.b.c1=appName
      a.b.c2=grailsHome
      """);

    GrailsTestCase.checkResolve(file, "a", "a");
  }
}
