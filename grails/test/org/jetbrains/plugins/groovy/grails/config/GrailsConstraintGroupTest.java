// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.groovy.grails.config;

import com.intellij.psi.PsiFile;
import org.jetbrains.plugins.groovy.grails.Grails14TestCase;

public class GrailsConstraintGroupTest extends Grails14TestCase {
  public void testCompletion() {
    PsiFile file = myFixture.addFileToProject("grails-app/conf/Config.groovy", """
      grails.gorm.default.constraints = {
          '*'(nullable:true, blank:false, size:1..20, <caret>)
      }
      """);

    checkCompletion(file, "creditCard", "inList", "min");
    checkNonExistingCompletionVariants("blank", "nullable");
  }

  public void testHighlighting() {
    PsiFile file = myFixture.addFileToProject("grails-app/conf/Config.groovy", """
      grails.gorm.default.constraints = {
          '*'(nullable:true, blank:"true", size: new Object())
      }
      """);
    myFixture.configureFromExistingVirtualFile(file.getVirtualFile());

    myFixture.checkHighlighting(true, false, true);
  }

  public void testCompletionShared() {
    myFixture.addFileToProject("grails-app/conf/Config.groovy", """
      grails.gorm.default.constraints = {
          '*'(nullable:true, blank:false, size:1..20)
          aaa1(nullable:true)
          aaa2(nullable:false)
      }
      """);

    PsiFile d = addDomain("""
                            class Ddd {
                              String name;
                              static constraints = {
                                name(shared: "<caret>")
                              }
                            }
                            """);

    checkCompletionVariants(d, "aaa1", "aaa2");
  }
}
