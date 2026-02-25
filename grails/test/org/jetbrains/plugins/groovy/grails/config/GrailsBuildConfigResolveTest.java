// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.groovy.grails.config;

import com.intellij.psi.PsiFile;
import org.jetbrains.plugins.groovy.codeInspection.assignment.GroovyAssignabilityCheckInspection;
import org.jetbrains.plugins.groovy.grails.GrailsTestCase;

public class GrailsBuildConfigResolveTest extends GrailsTestCase {
  @Override
  protected boolean useGrails14() {
    return true;
  }

  public void testResolve() {
    PsiFile file = myFixture.addFileToProject("grails-app/conf/BuildConfig.groovy", """
      grails.project.dependency.resolution = {
          pom false
      
          repositories {
              grailsPlugins()
              grailsHome()
              grailsCentral()
          }
      }
      """);

    GrailsTestCase.checkResolve(file, "grails");
  }

  public void testHighlighting() {
    myFixture.enableInspections(GroovyAssignabilityCheckInspection.class);

    PsiFile file = myFixture.addFileToProject("grails-app/conf/BuildConfig.groovy", """
      <warning descr="Grails configuration may be outdated">
      grails.project.dependency.resolution = {
          pom <warning descr="'pom' in 'org.codehaus.groovy.grails.resolve.config.DependencyConfigurationConfigurer' cannot be applied to '(java.lang.String)'">"true"</warning>
      
          inherits("global")
          inherits<warning descr="'inherits' in 'org.codehaus.groovy.grails.resolve.config.DependencyConfigurationConfigurer' cannot be applied to '(java.lang.Integer)'">(1)</warning>
      
          repositories {
              grailsPlugins()
              grailsHome<warning descr="'grailsHome' cannot be applied to '(java.lang.Boolean)'">(true)</warning>
              grailsCentral()
          }
      }
      </warning>""");
    myFixture.testHighlighting(true, false, true, file.getVirtualFile());
  }

  @Override
  protected boolean needBootstrap() {
    return true;
  }
}
