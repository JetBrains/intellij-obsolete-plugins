// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.groovy.grails;

import com.intellij.psi.PsiFile;

public class GrailsPageRendererTest extends Grails14TestCase {
  public void testCompletion() {
    addView("index.gsp", "...");
    addView("zzz/e.gsp", "...");

    PsiFile file = addService("""
                                class XxxService {
                                  grails.gsp.PageRenderer groovyPageRenderer
                                
                                  def foo() {
                                    groovyPageRenderer.render(view: ""\"/<caret>""\")
                                  }
                                }
                                """);
    checkCompletionVariants(file, "index", "zzz");
  }
}
