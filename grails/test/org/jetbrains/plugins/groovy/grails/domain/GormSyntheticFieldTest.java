// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.groovy.grails.domain;

import com.intellij.psi.PsiFile;
import junit.framework.TestCase;
import org.jetbrains.plugins.groovy.grails.GrailsTestCase;

public class GormSyntheticFieldTest extends GrailsTestCase {
  public void testRenameSimpleProperty() {
    PsiFile d = addDomain("""
                            
                            class City {
                            
                              static hasMany = [streets<caret>: String]
                            
                              static constraints = {
                                streets(maxSize: 2)
                              }
                            }
                            """);
    PsiFile c = addController("""
                                
                                class CccController {
                                  def index = {
                                    def x = new City().streets
                                    def y = new City().getStreets()
                                    new City().setStreets([])
                                  }
                                }
                                """);
    PsiFile j = myFixture.addFileToProject("grails-app/controllers/Jjj.java", """
      
      public class Jjj {
        static {
          new City().getStreets()
          new City().setStreets(null)
        }
      }
      """);

    myFixture.configureFromExistingVirtualFile(d.getVirtualFile());

    myFixture.renameElementAtCaret("sss");

    TestCase.assertEquals("""
                            
                            class City {
                            
                              static hasMany = [sss: String]
                            
                              static constraints = {
                                sss(maxSize: 2)
                              }
                            }
                            """, d.getText());

    TestCase.assertEquals("""
                            
                            class CccController {
                              def index = {
                                def x = new City().sss
                                def y = new City().getSss()
                                new City().setSss([])
                              }
                            }
                            """, c.getText());

    TestCase.assertEquals("""
                            
                            public class Jjj {
                              static {
                                new City().getSss()
                                new City().setSss(null)
                              }
                            }
                            """, j.getText());
  }

  public void testRenamePropertyWithField() {
    PsiFile d = addDomain("""
                            
                            class City {
                              Set streets;
                            
                              static hasMany = [streets<caret>: String]
                            
                              static constraints = {
                                streets(maxSize: 2)
                              }
                            }
                            """);
    PsiFile c = addController("""
                                
                                class CccController {
                                  def index = {
                                    def x = new City().streets
                                    def y = new City().getStreets()
                                    new City().setStreets([])
                                  }
                                }
                                """);
    PsiFile j = myFixture.addFileToProject("src/java/Jjj.java", """
      
      public class Jjj {
        static {
          new City().getStreets()
          new City().setStreets(null)
        }
      }
      """);

    myFixture.configureFromExistingVirtualFile(d.getVirtualFile());

    myFixture.renameElementAtCaret("sss");

    TestCase.assertEquals("""
                            
                            class City {
                              Set sss;
                            
                              static hasMany = [sss: String]
                            
                              static constraints = {
                                sss(maxSize: 2)
                              }
                            }
                            """, d.getText());

    TestCase.assertEquals("""
                            
                            class CccController {
                              def index = {
                                def x = new City().sss
                                def y = new City().getSss()
                                new City().setSss([])
                              }
                            }
                            """, c.getText());

    TestCase.assertEquals("""
                            
                            public class Jjj {
                              static {
                                new City().getSss()
                                new City().setSss(null)
                              }
                            }
                            """, j.getText());
  }
}
