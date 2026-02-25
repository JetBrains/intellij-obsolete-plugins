// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.groovy.grails.domain;

import com.intellij.psi.PsiFile;
import org.jetbrains.plugins.groovy.grails.GrailsTestCase;

public class GormNamedArgumentForHibernateMappingBuilderTest extends GrailsTestCase {
  public void testNamedArgumentForMethodCache() {
    PsiFile file = addDomain("""
                               
                               class Ddd {
                                 String name;
                               
                                 static mapping = {
                                   cache <caret>
                                 }
                               }
                               """);

    checkCompletion(file, "usage", "include");
  }

  @Override
  protected boolean needGormLibrary() {
    return true;
  }
}
