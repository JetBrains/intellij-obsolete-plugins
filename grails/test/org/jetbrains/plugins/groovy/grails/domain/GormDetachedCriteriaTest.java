// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.groovy.grails.domain;

import com.intellij.psi.PsiFile;
import org.jetbrains.plugins.groovy.grails.GrailsTestCase;

public class GormDetachedCriteriaTest extends GrailsTestCase {
  @Override
  protected boolean useGrails14() {
    return true;
  }

  public void testRename() throws Exception {
    addDomain("""
                
                class Ddd {
                  String name
                }
                """);

    configureBySimpleGroovyFile("""
                                  
                                  def x = new grails.gorm.DetachedCriteria<Ddd>(Ddd.class).build {
                                    eq "name", "Ivan"
                                    projections {
                                      if (true) {
                                        max("name")
                                      }
                                      eq "name", "1"
                                    }
                                  }
                                  
                                  x.and {
                                    eq "name", "Ivan"
                                  }
                                  
                                  x.eq("name", "Ivan")
                                  
                                  x = x.build({
                                    eq "name", "Ivan"
                                  })
                                  
                                  x.list(sort: 'name', {
                                      gt ""\"name""\", "a"
                                  })
                                  
                                  def z = Ddd.where {
                                      eq "name", "Ivan"
                                  }
                                  
                                  z.build {
                                    or {
                                      eq "name", "Vasya"
                                      or {
                                        eq "name", "Vasya"
                                      }
                                    }
                                  }
                                  
                                  Ddd.findAll [:], {
                                      eq "name", "Ivan"
                                      projections {
                                        if (true) {
                                          max("name")
                                        }
                                        eq "name", "1"
                                      }
                                  }
                                  
                                  Ddd.find {
                                      eq "name<caret>", "Ivan"
                                  }
                                  
                                  def g = new grails.gorm.DetachedCriteria<Ddd>(Ddd.class);
                                  g.updateAll(name: "Sergey")
                                  g.each { d ->
                                    println(d.name)
                                  }
                                  """);

    myFixture.renameElementAtCaret("firstName");

    myFixture.checkResult("""
                            
                            def x = new grails.gorm.DetachedCriteria<Ddd>(Ddd.class).build {
                              eq "firstName", "Ivan"
                              projections {
                                if (true) {
                                  max("firstName")
                                }
                                eq "firstName", "1"
                              }
                            }
                            
                            x.and {
                              eq "firstName", "Ivan"
                            }
                            
                            x.eq("firstName", "Ivan")
                            
                            x = x.build({
                              eq "firstName", "Ivan"
                            })
                            
                            x.list(sort: 'firstName', {
                                gt ""\"firstName""\", "a"
                            })
                            
                            def z = Ddd.where {
                                eq "firstName", "Ivan"
                            }
                            
                            z.build {
                              or {
                                eq "firstName", "Vasya"
                                or {
                                  eq "firstName", "Vasya"
                                }
                              }
                            }
                            
                            Ddd.findAll [:], {
                                eq "firstName", "Ivan"
                                projections {
                                  if (true) {
                                    max("firstName")
                                  }
                                  eq "firstName", "1"
                                }
                            }
                            
                            Ddd.find {
                                eq "firstName", "Ivan"
                            }
                            
                            def g = new grails.gorm.DetachedCriteria<Ddd>(Ddd.class);
                            g.updateAll(firstName: "Sergey")
                            g.each { d ->
                              println(d.firstName)
                            }
                            """);
  }

  public void testResolveDynamicFinderMethod() {
    PsiFile file = addDomain("""
                               
                               class Ddd {
                                 String firstName;
                                 String lastName;
                               
                                 static {
                                   def criteria = where {
                                     isNotNull("firstName")
                                   }
                               
                                   criteria.findByLastNameAndVersionBetween("Ivanov", 1, 2)
                                 }
                               }
                               """);
    GrailsTestCase.checkResolve(file);
  }

  public void testCompletionDynamicFinders() {
    configureByDomain("""
                        
                        class Ddd {
                          String firstName;
                          String lastName;
                        
                          static {
                            def criteria = where {
                              isNotNull("firstName")
                            }
                        
                            criteria.findByLastNameAnd<caret>
                          }
                        }
                        """);
    checkCompletion("findByLastNameAndVersion", "findByLastNameAndId");
  }
}
