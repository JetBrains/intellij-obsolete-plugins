// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.groovy.grails.reference.controller;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import org.jetbrains.plugins.grails.references.controller.ControllerMembersProvider;
import org.jetbrains.plugins.groovy.codeInspection.assignment.GroovyAssignabilityCheckInspection;
import org.jetbrains.plugins.groovy.grails.Grails14TestCase;

public class GrailsControllerAmbiguousMethodInspectionTest extends Grails14TestCase {
  public void testAmbiguousInspection() {
    myFixture.enableInspections(GroovyAssignabilityCheckInspection.class);

    configureByController("""
                            class CccController {
                              def foo() {
                                render(text: "text")
                              }
                            }
                            """);

    myFixture.checkHighlighting(true, false, true);
  }

  public void testResolve() {
    configureByController("""
                            class CccController {
                              def foo() {
                                render<caret>(text: "text")
                              }
                            }
                            """);

    PsiElement res = myFixture.getElementAtCaret();
    assertInstanceOf(res, PsiMethod.class);
    assertEquals(ControllerMembersProvider.CONTROLLER_API_CLASS, ((PsiMethod)res).getContainingClass().getQualifiedName());
  }
}
