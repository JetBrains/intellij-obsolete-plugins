// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.groovy.grails.domain;

import com.intellij.psi.PsiFile;
import junit.framework.TestCase;
import org.jetbrains.plugins.groovy.grails.GrailsTestCase;

public class GrailsDomainPropertiesTest extends GrailsTestCase {
  public void testRename() throws Exception {
    PsiFile fileSomeClass = addSimpleGroovyFile("""
                                                  
                                                  class SomeClass {
                                                   {
                                                    A a = new A()
                                                    a.manyProp = null
                                                   }
                                                  }
                                                  """);

    PsiFile fileA = addDomain("""
                                
                                class A {
                                  static hasMany = [manyProp<caret>: String]
                                }
                                """);

    myFixture.configureFromExistingVirtualFile(fileA.getVirtualFile());

    myFixture.renameElementAtCaret("ttt");

    TestCase.assertEquals("""
                            
                            class SomeClass {
                             {
                              A a = new A()
                              a.ttt = null
                             }
                            }
                            """, fileSomeClass.getText());

    TestCase.assertEquals("""
                            
                            class A {
                              static hasMany = [ttt: String]
                            }
                            """, fileA.getText());
  }

  public void testPropertyReferenceInListMethod() {
    configureByDomain("""
                        
                        class Ddd {
                          String firstName;
                          String lastName;
                        
                          static {
                            Ddd.list(sort:"<caret>")
                          }
                        }
                        """);

    checkCompletion("firstName", "lastName");
  }
}
