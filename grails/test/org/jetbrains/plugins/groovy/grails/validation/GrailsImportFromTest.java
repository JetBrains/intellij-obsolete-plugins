// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.groovy.grails.validation;

import org.jetbrains.plugins.groovy.codeInspection.assignment.GroovyAssignabilityCheckInspection;
import org.jetbrains.plugins.groovy.codeInspection.untypedUnresolvedAccess.GrUnresolvedAccessInspection;
import org.jetbrains.plugins.groovy.grails.GrailsTestCase;

public class GrailsImportFromTest extends GrailsTestCase {
  public void testCompletionImportFromDomain() {
    configureByDomain("""
                        class Ddd {
                            String name
                        
                            static constraints = {
                              <caret>
                            }
                        }
                        """);

    checkCompletion("name()", "importFrom()");
  }

  private void addValidateableAnnotationClass() {
    myFixture.addClass("""
                         package grails.validation;
                         @interface Validateable {}
                         """);
  }

  public void testCompletionImportFromValidatableObject() {
    addValidateableAnnotationClass();

    configureBySimpleGroovyFile("""
                                  import grails.validation.Validateable
                                  
                                  @Validateable
                                  class Aaa {
                                      String name
                                  
                                      static constraints = {
                                        <caret>
                                      }
                                  }
                                  """);

    checkCompletion("name", "importFrom()");
  }

  public void testImportFromHighlighting() {
    addValidateableAnnotationClass();

    myFixture.enableInspections(GroovyAssignabilityCheckInspection.class);
    myFixture.enableInspections(GrUnresolvedAccessInspection.class);

    addSimpleGroovyFile("""
                          import grails.validation.Validateable
                          @Validateable
                          class Foo {
                          }
                          """);

    configureBySimpleGroovyFile("""
                                  import grails.validation.Validateable
                                  @Validateable
                                  class Aaa {
                                      String name
                                  
                                      static constraints = {
                                        importFrom(Foo)
                                        importFrom(Foo, include: ['a'])
                                        importFrom(Foo, include: <warning>777</warning>)
                                        importFrom<warning>(12)</warning>
                                        <warning>importFrom123</warning>(12)
                                  
                                        name(notEqual: '_')
                                      }
                                  }
                                  """);

    myFixture.checkHighlighting(true, false, true);
  }

  public void testImportFromNamedArgumentsCompletion() {
    addValidateableAnnotationClass();

    addSimpleGroovyFile("""
                          import grails.validation.Validateable
                          @Validateable
                          class Foo {
                          }
                          """);

    configureBySimpleGroovyFile("""
                                  import grails.validation.Validateable
                                  @Validateable
                                  class Aaa {
                                      String name
                                  
                                      static constraints = {
                                        importFrom(Foo, <caret>)
                                      }
                                  }
                                  """);

    checkCompletion("include:", "exclude:");
  }
}
