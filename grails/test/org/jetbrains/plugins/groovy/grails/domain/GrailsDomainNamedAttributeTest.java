// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.groovy.grails.domain;

import com.intellij.psi.PsiFile;
import org.jetbrains.plugins.groovy.codeInspection.assignment.GroovyAssignabilityCheckInspection;
import org.jetbrains.plugins.groovy.grails.GrailsTestCase;

public class GrailsDomainNamedAttributeTest extends GrailsTestCase {
  @Override
  protected boolean needGormLibrary() {
    return true;
  }

  public void testCompletionListOrderBy() {
    PsiFile file = addDomain("""
                               
                               class Street {
                                 String name
                               
                                 static {
                                   Street.listOrderByName(<caret>)
                                 }
                               }
                               """);
    checkCompletion(file, "max", "offset", "sort", "fetch");
  }

  public void testHighlightListOrderBy() {
    myFixture.enableInspections(GroovyAssignabilityCheckInspection.class);

    PsiFile file = addDomain("""
                               
                               class Street {
                                 String name
                               
                                 static {
                                   Street.listOrderByName(max: '1', order: <warning descr="Type of argument 'order' can not be 'Boolean'">true</warning>, ignoreCase: <warning descr="Type of argument 'ignoreCase' can not be 'String'">'Yes'</warning>)
                                 }
                               }
                               """);
    myFixture.testHighlighting(true, false, true, file.getVirtualFile());
  }

  public void testCompletionList() {
    PsiFile file = addDomain("""
                               
                               class Street {
                                 String name
                               
                                 static {
                                   def c = Street.createCriteria()
                                   def res = c.list(<caret>) {}
                                 }
                               }
                               """);
    checkCompletion(file, "max", "offset", "sort", "fetch");
  }

  public void testHighlightList() {
    myFixture.enableInspections(GroovyAssignabilityCheckInspection.class);

    PsiFile file = addDomain("""
                               
                               class Street {
                                 String name
                               
                                 static {
                                   def c = Street.createCriteria()
                                   def res = c.list(max: '1', order: <warning descr="Type of argument 'order' can not be 'Boolean'">true</warning>, ignoreCase: <warning descr="Type of argument 'ignoreCase' can not be 'String'">'Yes'</warning>) {}
                                 }
                               
                                 static constraints = {
                                   def ss = "aaa$dd 32"
                                   name(matches: <warning descr="Type of argument 'matches' can not be 'GString'">ss</warning>)
                                 }
                               }
                               """);
    myFixture.testHighlighting(true, false, true, file.getVirtualFile());
  }
}
