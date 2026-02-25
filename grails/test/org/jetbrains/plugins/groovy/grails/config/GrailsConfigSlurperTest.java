// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.groovy.grails.config;

import com.intellij.psi.PsiFile;
import org.jetbrains.plugins.groovy.grails.GrailsTestCase;

public class GrailsConfigSlurperTest extends GrailsTestCase {
  public void testCompletionAfterMethodCall() {
    PsiFile file = addView("a.gsp", "${grailsApplication.getConfig().<caret>}");
    checkCompletion(file, "grails.project.groupId", "environments");
  }

  public void testCompletionAfterReference() {
    PsiFile file = addView("a.gsp", "${grailsApplication.config.<caret>}");
    checkCompletion(file, "grails.project.groupId", "environments");
  }

  public void testCompletionInner() {
    PsiFile file = addView("a.gsp", "${grailsApplication.config.grails.mime.<caret>}");
    checkCompletion(file, "file.extensions", "types", "use.accept.header");
  }

  public void testConfigurationHolder() throws Exception {
    configureBySimpleGroovyFile("org.codehaus.groovy.grails.commons.ConfigurationHolder.getConfig().environments.<caret>");
    checkCompletion("development", "test", "production");
  }

  public void testCompletionByDot() {
    configureByView("a.gsp", "${grailsApplication.getConfig().<caret>}");
    myFixture.completeBasic();
    myFixture.type("grai.pro.g=");
    myFixture.checkResult("${grailsApplication.getConfig().grails.project.groupId=}");
  }
}
