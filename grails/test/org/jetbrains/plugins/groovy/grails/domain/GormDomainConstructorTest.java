// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.groovy.grails.domain;

import com.intellij.psi.PsiFile;
import org.jetbrains.plugins.groovy.grails.GrailsTestCase;

public class GormDomainConstructorTest extends GrailsTestCase {
  public void test_no_recursive_invocation_message() {
    PsiFile domain = addDomain("""
                                 
                                 class Hello {
                                     String name
                                     int counter
                                 
                                     Hello(name, counter) {
                                       this()
                                       this.name = name
                                       this.counter = counter
                                     }
                                 }
                                 """);
    myFixture.testHighlighting(false, false, false, domain.getVirtualFile());
  }
}
