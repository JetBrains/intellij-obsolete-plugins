// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.groovy.grails.domain;

import com.intellij.psi.PsiFile;
import org.jetbrains.plugins.groovy.grails.GrailsTestCase;

public class GrailsInjectRegexTest extends GrailsTestCase {
  public void testCompletionRegex() {
    PsiFile file = addDomain("""
                               
                               class Street {
                                   String name
                               
                                   static constraints = {
                                       name(matches: "[A-Z][a-z\\\\<caret> .]+")
                                   }
                               }
                               """);
    checkCompletion(file, "A", "a", "B", "b");
  }
}
