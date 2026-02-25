// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.groovy.grails.inspections;

import org.jetbrains.plugins.groovy.codeInspection.declaration.GrMethodMayBeStaticInspection;
import org.jetbrains.plugins.groovy.grails.Grails14TestCase;

public class GrailsMethodMayBeStaticInspectionTest extends Grails14TestCase {
  public void testInspectionDisabled() {
    myFixture.enableInspections(GrMethodMayBeStaticInspection.class);
    configureByController("""
                            class CccController {
                                def index() {
                                  render("test")
                                }
                            
                                private foo() {
                                  render("test")
                                }
                            
                                private <warning>bar</warning>() {
                                  def z = 12
                                }
                            }
                            """);
    myFixture.checkHighlighting(true, false, true);
  }
}
