// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.groovy.grails.pluginSupport;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.roots.ContentEntry;
import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiModifier;
import com.intellij.testFramework.UsefulTestCase;
import junit.framework.TestCase;
import org.jetbrains.plugins.groovy.codeInspection.assignment.GroovyAssignabilityCheckInspection;
import org.jetbrains.plugins.groovy.grails.GrailsTestCase;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class GrailsSearchableTest extends GrailsTestCase {
  @Override
  protected void configureModule(Module module, ModifiableRootModel model, ContentEntry contentEntry) {
    try {
      VirtualFile applicationProperties = myFixture.getTempDirFixture().findOrCreateDir("application.properties");
      applicationProperties.setBinaryContent("plugins.searchable=0.5.5.1".getBytes(StandardCharsets.UTF_8));
    }
    catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public void testSearchableFieldCompletion() {
    configureByDomain("""
                        class Ddd {
                          static s<caret>
                        }
                        """);
    checkCompletion("searchable");
  }

  public void testFieldReference1() {
    configureByDomain("""
                        class Ddd {
                          String sss
                        
                          static searchable = [only: '<caret>']
                        }
                        """);
    checkCompletion("sss");
  }

  public void testFieldReference2() {
    configureByDomain("""
                        class Ddd {
                          String sss1
                          String sss2
                        
                          static searchable = [only: ['sss1', '<caret>']]
                        }
                        """);
    checkCompletion("sss2");
    checkNonExistingCompletionVariants("sss1");
  }

  public void testFieldReference3() {
    configureByDomain("""
                        class Ddd {
                          String sss1
                          String sss2
                        
                          static searchable = {
                            only = ['sss1', '<caret>']
                          }
                        }
                        """);
    checkCompletion("sss2");
    checkNonExistingCompletionVariants("sss1");
  }

  public void testRenameField() {
    configureByDomain("""
                        class Ddd {
                          String sss1<caret>
                          String sss2
                        
                          static searchable = {
                            only = ['sss1', 'sss1', 'sss2', 'sss1']
                          }
                        }
                        """);
    myFixture.renameElementAtCaret("z");

    myFixture.checkResult("""
                            class Ddd {
                              String z
                              String sss2
                            
                              static searchable = {
                                only = ['z', 'z', 'sss2', 'z']
                              }
                            }
                            """);
  }

  public void testResolveOnlyAndExpectReferences() {
    PsiFile file = addDomain("""
                               class Ddd {
                                 String sss1
                                 String sss2
                               
                                 static searchable = {
                                   only = ['sss1', 'sss1', 'sss2', 'sss1']
                                   zzz = 1;
                                 }
                               }
                               """);
    GrailsTestCase.checkResolve(file, "zzz");
  }

  public void testPropertyAsMethodCompletion() {
    configureByDomain("""
                        class Ddd {
                          String sss1
                          String sss2
                        
                          static searchable = {
                            <caret>
                          }
                        }
                        """);
    checkCompletion("sss1", "sss2");
  }

  public void testDomainMethodsCompletion1() {
    configureByDomain("""
                        class Ddd {
                          String sss1
                        
                          static def xxx() {
                            Ddd.<caret>
                          }
                        }
                        """);
    checkCompletion("xxx");
    checkNonExistingCompletionVariants("termFreqs", "countHits", "search");
  }

  public void testDomainMethodsCompletion2() {
    configureByDomain("""
                        class Ddd {
                          String sss1
                          static searchable = true
                        
                          static def xxx() {
                            Ddd.<caret>
                          }
                        }
                        """);
    checkCompletion("termFreqs", "countHits", "search");
    checkNonExistingCompletionVariants();
  }

  public void testStaticMethodResolve() {
    configureByDomain("""
                        class Ddd {
                          String sss1
                          static searchable = true
                        
                          static def xxx() {
                            Ddd.index<caret>()
                          }
                        }
                        """);
    PsiElement element = myFixture.getElementAtCaret();
    UsefulTestCase.assertInstanceOf(element, PsiMethod.class);
    TestCase.assertTrue(((PsiMethod)element).hasModifierProperty(PsiModifier.STATIC));
  }

  public void testNonStaticMethodResolve() {
    configureByDomain("""
                        class Ddd {
                          String sss1
                          static searchable = true
                        
                          static def xxx() {
                            Ddd d = new Ddd();
                            d.index<caret>()
                          }
                        }
                        """);
    PsiElement element = myFixture.getElementAtCaret();
    UsefulTestCase.assertInstanceOf(element, PsiMethod.class);
    TestCase.assertFalse(((PsiMethod)element).hasModifierProperty(PsiModifier.STATIC));
  }

  public void testNonStaticMethodCompletion() {
    configureByDomain("""
                        class Ddd {
                          String sss1
                          static searchable = true
                        
                          static def xxx() {
                            Ddd d = new Ddd();
                            d.<caret>
                          }
                        }
                        """);
    checkCompletion("index");
  }

  public void testStaticMethodCompletion() {
    configureByDomain("""
                        class Ddd {
                          String sss1
                          static searchable = true
                        
                          static def xxx() {
                            Ddd.<caret>
                          }
                        }
                        """);
    checkCompletion("index");
  }

  public void testReturnType() {
    addService("""
                 class SearchableService {
                     boolean transactional = true
                     def compass
                     def compassGps
                     def searchableMethodFactory
                 
                     def search(Object[] args) {}
                     def searchEvery(Object[] args) {}
                     def searchTop(Object[] args) {}
                     def moreLikeThis(Object[] args) {}
                     def countHits(Object[] args) {}
                     def termFreqs(Object[] args) {}
                     def suggestQuery(Object[] args) {}
                     def rebuildSpellingSuggestions(options) {}
                     def indexAll(Object[] args) {}
                     def index(Object[] args) {}
                     def unindexAll(Object[] args) {}
                     def unindex(Object[] args) {}
                     def reindexAll(Object[] args) {}
                     def reindex(Object[] args) {}
                     def startMirroring() {}
                     def stopMirroring() {}
                 }
                 """);

    PsiFile file = addDomain("""
                               class Ddd {
                                 String name;
                               
                                 SearchableService searchableService
                               
                                 static searchable = true;
                               
                                 def xxx() {
                                   Ddd.search("*").containsKey("aaa")
                                   Ddd.search("*", [result: "count"]).byteValue()
                                   Ddd.search("*", [result: "top"]).getName()
                                   Ddd.search("*", [result: "searchResult"]).containsKey("aaa")
                                   Ddd.search("*", [result: "every"]).listIterator().next().name
                                   Ddd.search([result: "top"], "*").getName()
                               
                                   new Ddd().moreLikeThis([result: "top"]).getName()
                                   Ddd.moreLikeThis(new Ddd(), [result: "top"]).getName()
                               
                                   Map m = unresolvedMap
                                   Ddd.search(m, "*").containsValue("aaa")
                               
                                   searchableService.search("*", [result: "count"]).byteValue()
                                   searchableService.search([result: "every"], "*").listIterator()
                               
                                   searchableService.moreLikeThis([result: 'count']).byteValue()
                               
                                   Ddd.search("*", result: "every").listIterator().next().name
                                   searchableService.moreLikeThis(result: 'count').byteValue()
                                 }
                               }
                               """);
    GrailsTestCase.checkResolve(file, "unresolvedMap", "containsValue");
  }

  public void testCompletionMapKeys() {
    configureByDomain("""
                        class Ddd {
                          String name
                        
                          static searchable = true;
                        
                          def xxx() {
                            Ddd.search("*").<caret>
                          }
                        }
                        """);
    checkCompletion("results", "scores", "max");
  }

  public void testTypeMapKeys() {
    configureByDomain("""
                        class Ddd {
                          String name
                        
                          static searchable = true;
                        
                          def xxx() {
                            Ddd.search("*").results.iterator().next().<caret>
                          }
                        }
                        """);

    checkCompletion("name", "id", "version");
  }

  public void testMethodAll() {
    myFixture.enableInspections(GroovyAssignabilityCheckInspection.class);

    PsiFile file = configureByDomain("""
                                       class Ddd {
                                         String name
                                       
                                         static searchable = {
                                           name(aaa: 's')
                                           all(zzz: 's')
                                         };
                                       }
                                       """);

    GrailsTestCase.checkResolve(file);
    myFixture.checkHighlighting(true, false, true);
  }
}
