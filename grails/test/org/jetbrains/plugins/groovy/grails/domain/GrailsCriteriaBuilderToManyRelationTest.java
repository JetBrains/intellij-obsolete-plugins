// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.groovy.grails.domain;

import com.intellij.psi.PsiFile;
import junit.framework.TestCase;
import org.jetbrains.plugins.groovy.grails.GrailsTestCase;

public class GrailsCriteriaBuilderToManyRelationTest extends GrailsTestCase {
  private void initDomains() {
    addDomain("""
                
                class Human {
                  String humanName;
                }
                """);

    addDomain("""
                
                class Street {
                  String streetName;
                  static hasMany = [humans: Human]
                }
                """);
    addDomain("""
                
                class City {
                  String cityName;
                  static hasMany = [streets: Street]
                }
                """);
  }

  @Override
  protected boolean needGormLibrary() {
    return true;
  }

  @Override
  protected boolean needHibernate() {
    return true;
  }

  public void testCompletion0() throws Exception {
    initDomains();

    configureBySimpleGroovyFile("""
                                  
                                  City.withCriteria {
                                    <caret>
                                  }
                                  """);

    checkCompletion("eq", "streets");
  }

  public void testCompletion1() throws Exception {
    initDomains();

    configureBySimpleGroovyFile("""
                                  
                                  City.withCriteria {
                                    streets {
                                      <caret>
                                    }
                                  }
                                  """);

    checkCompletion("eq", "humans");
  }

  public void testCompletion2() throws Exception {
    initDomains();

    configureBySimpleGroovyFile("""
                                  
                                  def c = City.createCriteria()
                                  c {
                                    streets {
                                      eq "<caret>"
                                    }
                                  }
                                  """);

    checkCompletion("streetName");
    checkNonExistingCompletionVariants("humanName", "cityName", "city");
  }

  public void testCompletion3() throws Exception {
    initDomains();

    configureBySimpleGroovyFile("""
                                  
                                  def c = City.createCriteria()
                                  c {
                                    streets {
                                      humans {
                                        eq "<caret>"
                                      }
                                    }
                                  }
                                  """);

    checkCompletion("humanName");
    checkNonExistingCompletionVariants("cityName", "streetName", "humans");
  }

  public void testRename1() throws Exception {
    initDomains();

    PsiFile file = addSimpleGroovyFile("""
                                         
                                         (City.createCriteria()) {
                                           streets {
                                             humans {
                                               eq "humanName", "Vasya"
                                             }
                                           }
                                         }
                                         """);

    configureBySimpleGroovyFile("new Human().humanName<caret>");
    myFixture.renameElementAtCaret("hhh");

    TestCase.assertEquals("""
                            
                            (City.createCriteria()) {
                              streets {
                                humans {
                                  eq "hhh", "Vasya"
                                }
                              }
                            }
                            """, file.getText());
  }

  public void testRename2() throws Exception {
    initDomains();

    PsiFile file = addSimpleGroovyFile("""
                                         
                                         (City.createCriteria()) {
                                           streets {
                                             humans {
                                               eq "humanName", "Vasya"
                                             }
                                           }
                                         }
                                         """);

    configureBySimpleGroovyFile("new Street().humans<caret>");
    myFixture.renameElementAtCaret("hhh");

    TestCase.assertEquals("""
                            
                            (City.createCriteria()) {
                              streets {
                                hhh {
                                  eq "humanName", "Vasya"
                                }
                              }
                            }
                            """, file.getText());
  }
}
