// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.groovy.grails.reference.taglib;

import com.intellij.psi.PsiFile;
import org.jetbrains.plugins.groovy.grails.GrailsTestCase;

/**
 * @author user
 */
public class GrailsTaglibParamTypeTest extends GrailsTestCase {
  public void testResolve() {
    PsiFile file = addTaglib("""
                               class MyTagLib {
                                   def xxx = { attr, body ->
                                       def x = attr.unresolved1
                                       def y = body.unresolved2
                                       out << attr.size() + body.call()
                                   }
                               }
                               """);

    GrailsTestCase.checkResolve(file, "unresolved2");
  }
}
