// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.groovy.grails.reference.taglib;

import com.intellij.psi.PsiFile;
import org.jetbrains.plugins.groovy.grails.Grails14TestCase;

public class GrailsTaglib14Test extends Grails14TestCase {
  public void testCompletion() {
    PsiFile taglibClass = addTaglib("""
                                      class MyTagLib {
                                        def xxx = {
                                          <caret>
                                        }
                                      }
                                      """);
    checkCompletion(taglibClass, "log", "throwTagError", "webRequest", "actionName");
  }
}
