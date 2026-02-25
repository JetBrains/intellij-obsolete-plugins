// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.groovy.grails.gsp;

import org.jetbrains.plugins.groovy.codeInspection.bugs.GroovyDivideByZeroInspection;
import org.jetbrains.plugins.groovy.grails.GrailsTestCase;

public class GspGroovyInspectionTest extends GrailsTestCase {
  public void testGroovyInspections() {
    myFixture.configureByText("a.gsp", "<% def one = <warning descr=\"Division by zero\">2 / 0</warning> %>");
    myFixture.enableInspections(GroovyDivideByZeroInspection.class);
    myFixture.checkHighlighting(true, false, true);
  }
}
