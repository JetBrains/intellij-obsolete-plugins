// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.groovy.grails.domain;

import com.intellij.psi.PsiFile;
import org.jetbrains.plugins.groovy.grails.GrailsTestCase;

public class GrailsTransientPropertyTest extends GrailsTestCase {
  public void _testCompletion() {
    myFixture.addFileToProject("src/java/aaa/Parent.java", """
      
      package aaa;
      
      public class Parent {
          public String getSss() {
              return "sss";
          }
      }
      """);

    PsiFile file = addDomain("""
                               
                               class City extends aaa.Parent {
                               
                                   String name
                                   int peopleCount
                                   Set<String> street;
                               
                                   static hasMany = [setOfString: String]
                               
                                   static transients = ["<caret>", 'street']
                               
                                   public String getSss2523() {
                               
                                   }
                               }
                               """);

    checkCompletionVariants(file, "name", "peopleCount", "sss2523", "sss");
  }

  public void testRenameMethod() {
    configureByDomain("""
                        
                        class City {
                        
                            String name
                            int peopleCount
                            Set<String> street;
                        
                            static transients = ["sss", 'street']
                        
                            public String getSss<caret>() {
                        
                            }
                        }
                        """);

    myFixture.renameElementAtCaret("getS");

    myFixture.checkResult("""
                            
                            class City {
                            
                                String name
                                int peopleCount
                                Set<String> street;
                            
                                static transients = ["s", 'street']
                            
                                public String getS() {
                            
                                }
                            }
                            """);
  }

  public void testRenameProperty() {
    configureByDomain("""
                        
                        class City {
                            String name
                            static transients = ["name<caret>"]
                        }
                        """);

    myFixture.renameElementAtCaret("cityName");

    myFixture.checkResult("""
                            
                            class City {
                                String cityName
                                static transients = ["cityName"]
                            }
                            """);
  }

  public void testCollectionPropertyWithoutHasManyIsTransient() {
    addDomain("""
                
                class City {
                    String name
                    Collection<String> street;
                }
                """);
    configureByController("""
                            
                            class CccController {
                              def index = {
                                City.withCriteria({
                                  eq "<caret>"
                                })
                              }
                            }
                            """);

    checkCompletion("id", "version", "name");
    checkNonExistingCompletionVariants("street");
  }

  @Override
  protected boolean needGormLibrary() {
    return true;
  }

  @Override
  protected boolean needHibernate() {
    return true;
  }
}
