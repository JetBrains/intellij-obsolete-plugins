// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.groovy.grails.reference.controller;

import com.intellij.psi.PsiFile;
import org.jetbrains.plugins.groovy.grails.Grails14TestCase;

public class GrailsNamespaceTest extends Grails14TestCase {
  public void testControllerNamespaceField() {
    addController("""
                    
                    class AaaController {
                      static namespace="aaa1"
                    }
                    """);
    addController("""
                    
                    class BbbController {
                      static namespace="aaa2"
                    }
                    """);

    PsiFile c = addController("""
                                
                                class CccController {
                                  static namespace="aaa<caret>"
                                }
                                """);
    checkCompletionVariants(c, "aaa1", "aaa2");
  }
}
