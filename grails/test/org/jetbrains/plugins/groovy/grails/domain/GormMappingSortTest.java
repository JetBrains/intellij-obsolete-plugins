// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.groovy.grails.domain;

import org.jetbrains.plugins.groovy.codeInspection.assignment.GroovyAssignabilityCheckInspection;
import org.jetbrains.plugins.groovy.grails.GrailsTestCase;

public class GormMappingSortTest extends GrailsTestCase {
  @Override
  protected boolean needGormLibrary() {
    return true;
  }

  public void testCompletion() {
    configureByDomain("""
                        
                        class Ddd {
                          String firstName
                          String lastName
                        
                          static mapping = {
                            sort <caret>
                          }
                        }
                        """);

    checkCompletion("firstName:", "lastName:");
  }

  public void testHighlighting() {
    myFixture.enableInspections(GroovyAssignabilityCheckInspection.class);

    configureByDomain("""
                        
                        class Ddd {
                          int firstName
                          int lastName
                        
                          static mapping = {
                            sort firstName: <warning>1</warning>, lastName: "zzz"
                          }
                        
                        }
                        """);

    myFixture.checkHighlighting(true, false, true);
  }

  public void testSortNamedArgumentCompletion() {
    configureByDomain("""
                        
                        class Ddd {
                          String name
                          static hasMany = [aaa: Ddd]
                        
                          static mapping = {
                            aaa(sort: '<caret>')
                          }
                        }
                        """);

    checkCompletion("name", "id");
  }
}
