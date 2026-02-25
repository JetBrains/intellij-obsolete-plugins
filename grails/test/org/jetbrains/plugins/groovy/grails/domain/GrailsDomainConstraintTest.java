// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.groovy.grails.domain;

import com.intellij.codeInsight.completion.impl.CamelHumpMatcher;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiReference;
import com.intellij.testFramework.UsefulTestCase;
import junit.framework.TestCase;
import org.jetbrains.plugins.groovy.codeInspection.assignment.GroovyAssignabilityCheckInspection;
import org.jetbrains.plugins.groovy.grails.GrailsTestCase;

public class GrailsDomainConstraintTest extends GrailsTestCase {
  public void testHighlighting() {
    addDomain("""
                
                class Eee {
                
                  Ddd ddd;
                
                  static constraints = {
                  }
                }
                """);

    addDomain("""
                
                class Ddd {
                
                  String sss
                
                  static hasOne = [oneToOne: Eee]
                
                  static constraints = {
                    sss blank:false
                    sss(12323)
                  }
                
                  static mapping = {
                    table 'tableName'
                    oneToOne nullable: false;
                  }
                }
                """);

    myFixture.testHighlighting(true, false, true, "grails-app/domain/Ddd.groovy");
  }

  public void testRename() {
    configureByDomain("""
                        
                        class Ddd {
                        
                          String sss<caret>
                        
                          static hasOne = [oneToOne: Eee]
                        
                          static int sss(int x) {
                            return x;
                          }
                        
                          static constraints = {
                            sss blank:false
                          }
                        
                          static mapping = {
                            table 'tableName'
                            oneToOne nullable: false;
                            sss type:'someText'
                            sss(2)
                          }
                        }
                        """);

    myFixture.renameElementAtCaret("theField");

    myFixture.checkResult("""
                            
                            class Ddd {
                            
                              String theField
                            
                              static hasOne = [oneToOne: Eee]
                            
                              static int sss(int x) {
                                return x;
                              }
                            
                              static constraints = {
                                theField blank:false
                              }
                            
                              static mapping = {
                                table 'tableName'
                                oneToOne nullable: false;
                                theField type:'someText'
                                sss(2)
                              }
                            }
                            """);
  }

  public void testRename2() {
    configureByDomain("""
                        
                        class Ddd {
                          String sss<caret>
                        
                          static int sss(Map map) {
                            return map.size()
                          }
                        
                          static constraints = {
                            sss blank:false
                          }
                        
                          static mapping = {
                            table 'tableName'
                            sss type:'someText'
                          }
                        }
                        """);

    myFixture.renameElementAtCaret("theField");

    myFixture.checkResult("""
                            
                            class Ddd {
                              String theField
                            
                              static int sss(Map map) {
                                return map.size()
                              }
                            
                              static constraints = {
                                sss blank:false
                              }
                            
                              static mapping = {
                                table 'tableName'
                                sss type:'someText'
                              }
                            }
                            """);
  }

  public void testCompletion() {
    CamelHumpMatcher.forceStartMatching(myFixture.getTestRootDisposable());
    PsiFile ddd = addDomain("""
                              
                              class Ddd {
                                String discrimin555
                              
                                static mapping = {
                                  table 'tableName'
                                  discrimin<caret>
                                }
                              }
                              """);

    checkCompletion(ddd, "discrimin555()");

    PsiFile ddd2 = addDomain("""
                               
                               class Ddd2 {
                                 String field
                               
                                 static mapping = {
                                   table 'tableName'
                                   field(u<caret>)
                                 }
                               }
                               """);

    checkCompletionVariants(ddd2, "unique", "updateable");
  }

  private void doTestReferenceProvider(String classText) {
    addDomain(classText);

    PsiReference ref = myFixture.getReferenceAtCaretPositionWithAssertion("grails-app/domain/Ddd.groovy");

    PsiElement resolve = ref.resolve();

    UsefulTestCase.assertInstanceOf(resolve, PsiClass.class);
    TestCase.assertEquals("MaxSizeConstraint", ((PsiClass)resolve).getName());
  }

  public void testReferencesToConstraintsClasses1() {
    doTestReferenceProvider("""
                              
                              class Ddd {
                                  String name
                              
                                  static constraints = {
                                      name(maxSiz<caret>e: 20)
                                  }
                              }
                              """);
  }

  public void testReferencesToConstraintsClasses2() {
    doTestReferenceProvider("""
                              
                              class Ddd {
                                  String name
                              
                                  static constraints = {
                                      name([maxSiz<caret>e: 20])
                                  }
                              }
                              """);
  }

  public void testReferencesToConstraintsClasses3() {
    doTestReferenceProvider("""
                              
                              class Ddd {
                                  String name
                              
                                  static constraints = {
                                      name("maxSiz<caret>e": 20)
                                  }
                              }
                              """);
  }

  private void doTestHighlighting(String text) {
    myFixture.enableInspections(GroovyAssignabilityCheckInspection.class);
    PsiFile file = myFixture.addFileToProject("grails-app/domain/Ddd.groovy", text);
    myFixture.testHighlighting(true, false, true, file.getVirtualFile());
  }

  public void testAnnonation1() {
    doTestHighlighting("""
                         
                         class Street {
                             String name
                         
                             static constraints = {
                                 name(<warning descr="Constraint 'minSize' already defined for field 'name'">minSize</warning> : 1)
                                 name("minSize" : 3)
                             }
                         }
                         """);
  }

  public void testAnnonation2() {
    doTestHighlighting("""
                         
                         class Street {
                             String name
                             String field
                             String field2
                             String ttt
                             float float1
                             Float float2
                             Float float3
                             Double float4
                         
                             static mapping = {
                                 name(maxSize: "aaa")
                             }
                         
                             static constraints = {
                                 name(minSize: <warning descr="Type of argument 'minSize' can not be 'String'">"aaa"</warning>, "maxSize": <warning descr="Type of argument 'maxSize' can not be 'String'">"aaa"</warning>, url: ["a", "b", "c"])
                                 field('maxSize': <warning descr="Type of argument 'maxSize' can not be 'String'">"aaa"</warning>, min: new Object() , url: Boolean.TRUE)
                                 field2 minSize: <warning descr="Type of argument 'minSize' can not be 'Long'">12L</warning>, maxSize: new Integer(12)
                                 name(unique: ['field', 'field2'])
                                 ttt(unique: 'field')
                                 float1(scale: <warning>2d</warning>)
                                 float2(scale: <warning>2d</warning>)
                                 float3(scale: 2)
                         
                                 float1(min: <warning>1</warning>, max: <warning>2d</warning>)
                                 float2(min: <warning>1</warning>, max: <warning>2d</warning>)
                                 float3(min: 0f, max: 2f)
                                 float4(min: new Double(0f), max: new Double(1f))
                             }
                         }
                         """);
  }

  public void testConstraintValidatorArgumentType1() {
    configureByDomain("""
                        
                        class Ddd {
                          String field
                        
                          static constraints = {
                            field(validator: {val, obj, e ->
                              val.<caret>
                            })
                          }
                        }
                        """);

    checkCompletion("substring", "length");
  }

  public void testConstraintValidatorArgumentType2() {
    configureByDomain("""
                        
                        class Ddd {
                          String field
                        
                          static constraints = {
                            field([validator: {val, obj, e ->
                              obj.<caret>
                            }])
                          }
                        }
                        """);

    checkCompletion("field");
  }

  public void testConstraintValidatorArgumentType3() {
    configureByDomain("""
                        
                        class Ddd {
                          String field
                        
                          static constraints = {
                            field(validator: {
                              it.<caret>
                            })
                          }
                        }
                        """);

    checkCompletion("substring", "length");
  }

  public void testCompletionPropertyNameInConstraints() {
    PsiFile file = addDomain("""
                               
                               class Ddd {
                                 String foo;
                                 String transientProperty;
                               
                                 def xxx;
                               
                                 static hasMany = [sss: String]
                               
                                 static transients = ['transientProperty']
                               
                                 static constraints = {
                                   <caret>
                                 }
                               }
                               
                               """);

    checkCompletion(file, "foo", "sss", "transientProperty");
  }

  @Override
  protected boolean needGormLibrary() {
    return true;
  }
}
