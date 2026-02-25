// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.groovy.grails.domain;

import com.intellij.openapi.editor.markup.RangeHighlighter;
import com.intellij.psi.PsiFile;
import com.intellij.testFramework.UsefulTestCase;
import com.intellij.usageView.UsageInfo;
import junit.framework.TestCase;
import org.jetbrains.plugins.groovy.codeInspection.assignment.GroovyAssignabilityCheckInspection;
import org.jetbrains.plugins.groovy.grails.GrailsTestCase;

import java.util.Collection;

public class GrailsDomainFinderMethodsTest extends GrailsTestCase {
  public void testResolveAndHighlighting() {
    myFixture.enableInspections(GroovyAssignabilityCheckInspection.class);

    addDomain("""
                
                class Ddd {
                  int field
                  int aaaAnd
                  int andbbb
                  int between
                  int pHash
                }
                
                """);
    PsiFile file = addController("""
                                   
                                   class CccController {
                                     def index = {
                                       Ddd.findByBetweenBetween(1, 3, [:])
                                       Ddd.findByBetweenBetween(1, 3)
                                       Ddd.findByBetweenNotBetween(1, 3)
                                       Ddd.findByAaaAnd(3)
                                       Ddd.findByFieldNot()
                                       Ddd.findByAaaAndField(3)
                                       Ddd.countByFieldNotLessThanAndAaaAndBetween(1, 2, 6)
                                       Ddd.findAllByFieldBetweenOrIdNotEqual(1, 2, 4)
                                       Ddd.findAllByFieldBetweenOrBetween(1, 2, 4)
                                       Ddd.findAllByFieldBetweenOrVersionBetween(1, 2, 4, 7)
                                       Ddd.countByFieldBetween<warning descr="'countByFieldBetween' in 'Ddd' cannot be applied to '(java.lang.Integer)'">(1)</warning>
                                       int x = Ddd.countByFieldNotIsNotNull()
                                       assert x != 0
                                       Ddd <warning>d</warning> = Ddd.countByField(1)
                                       assert d != null
                                       d = Ddd.findByField(1)
                                       assert d != null
                                       List<Ddd> list = Ddd.findAllByFieldIsNull()
                                       assert list != null
                                       <warning>d</warning> = Ddd.findAllByFieldIsNotNull()
                                       assert d != null
                                       Ddd.countByIdAndVersionAndFieldGreaterThanAndFieldEqual(1, 2, 5, 6)
                                       Ddd.countByIdAndVersionAndFieldGreaterThanOrFieldEqual(1, 2, 5, 6)
                                       Ddd.countBypHash(1)
                                       Ddd.countByPHash(1)
                                     }
                                   }
                                   """);
    myFixture.testHighlighting(true, false, true, file.getVirtualFile());
    GrailsTestCase.checkResolve(file, "findByFieldNot", "findByAaaAndField", "countByFieldNotLessThanAndAaaAndBetween",
                                "countByIdAndVersionAndFieldGreaterThanOrFieldEqual");
  }

  public void testRename() {
    PsiFile controller = addController("""
                                         
                                         import ddd.ddd.Ddd;
                                         
                                         class CccController {
                                           def index = {
                                             Ddd.findAllByAaa(1)
                                             Ddd.findAllByAaaAndBbb(1, 2)
                                             Ddd.findByBbbLessThanOrAaa(1, 2)
                                             Ddd.countByAaaNotEqual(1)
                                             Ddd.countByAaaNotEqualAndAaaBetween(1, 2, 4)
                                             Ddd.countByAaaNotEqualAndAaaBetweenAndIdEqualAndVersionAndAaaGreaterThan(1, 2, 4, 11, 1, 0)
                                           }
                                         }
                                         """);

    PsiFile controller2 = addController("""
                                          
                                          class SssController {
                                            def index = {
                                              ddd.ddd.Ddd.countByAaaNotEqualAndAaaBetween(1, 2, 4)
                                            }
                                          }
                                          """);

    PsiFile domainFile = configureByDomain("""
                                             
                                             package ddd.ddd
                                             
                                             class Ddd {
                                               int aaa<caret>
                                               int bbb
                                             }
                                             """);

    myFixture.renameElementAtCaret("zzz111");

    TestCase.assertEquals("""
                            
                            package ddd.ddd
                            
                            class Ddd {
                              int zzz111
                              int bbb
                            }
                            """, domainFile.getText());

    TestCase.assertEquals("""
                            
                            import ddd.ddd.Ddd;
                            
                            class CccController {
                              def index = {
                                Ddd.findAllByZzz111(1)
                                Ddd.findAllByZzz111AndBbb(1, 2)
                                Ddd.findByBbbLessThanOrZzz111(1, 2)
                                Ddd.countByZzz111NotEqual(1)
                                Ddd.countByZzz111NotEqualAndZzz111Between(1, 2, 4)
                                Ddd.countByZzz111NotEqualAndZzz111BetweenAndIdEqualAndVersionAndZzz111GreaterThan(1, 2, 4, 11, 1, 0)
                              }
                            }
                            """, controller.getText());

    TestCase.assertEquals("""
                            
                            class SssController {
                              def index = {
                                ddd.ddd.Ddd.countByZzz111NotEqualAndZzz111Between(1, 2, 4)
                              }
                            }
                            """, controller2.getText());
  }

  public void testFindUsages() {
    addDomain("""
                
                class Ddd {
                  String name
                }
                """);

    addDomain("""
                
                class Ddd2 {
                  String name
                }
                """);

    addController("""
                    
                    class Ccc1Controller {
                      def index = {
                        render(Ddd.findAllByName("aaa"))
                      }
                      def foo = {
                        render(Ddd.findAllByName("bbb"))
                        render(Ddd2.findAllByName("bbb"))
                        render(Ddd2.findAllByName("bbb"))
                        render(Ddd2.findAllByName("bbb"))
                        render(Ddd2.findAllByName("bbb"))
                        render(Ddd2.findAllByName("bbb"))
                      }
                    }
                    """);

    configureByController("""
                            
                            class Ccc2Controller {
                              def index = {
                                render(Ddd.findAllByName<caret>("xxx"))
                              }
                            }
                            """);

    Collection<UsageInfo> res = myFixture.findUsages(myFixture.getElementAtCaret());
    UsefulTestCase.assertSize(3, res);
  }

  public void testHighlightUsages() {
    addDomain("""
                
                class Ddd {
                  String name
                }
                """);
    addDomain("""
                
                class Ddd2 {
                  String name
                }
                """);

    PsiFile c = addController("""
                                
                                class CccController {
                                  def index = {
                                    render(Ddd.findAllByName("aaa"))
                                    render(Ddd.findAllByName(""))
                                    render(Ddd2.findAllByName(""))
                                    render(Ddd2.findAllByName(""))
                                    render(Ddd2.findAllByName(""))
                                  }
                                  def foo = {
                                    render(Ddd.findAllByName<caret>("bbb"))
                                  }
                                }
                                """);

    RangeHighlighter[] res = myFixture.testHighlightUsages(getFilePath(c));
    UsefulTestCase.assertSize(3, res);
  }
}
