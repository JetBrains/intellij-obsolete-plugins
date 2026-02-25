// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.groovy.grails.domain;

import com.intellij.psi.PsiFile;
import org.jetbrains.plugins.groovy.grails.GrailsTestCase;

public class GrailsGormGetIdMethodTest extends GrailsTestCase {
  public void testFieldFromParent() {
    addDomain("class Nnn {}");
    addDomain("class Ddd {}");

    myFixture.addFileToProject("grails-app/domain/AbstractDomain.java", """
      
      public abstract class AbstractDomain {
        private Ddd ddd;
      
        public Ddd getDdd() {
          return ddd;
        }
      
        public void setDdd(Ddd ddd) {
          this.ddd = ddd;
        }
      }
      """);

    addDomain("""
                
                class Xxx extends AbstractDomain {
                  Nnn nnn;
                
                  static transients = ["nnn"]
                }
                """);

    configureByController("""
                            
                            class CccController {
                              def index = {
                                new Xxx().<caret>
                              }
                            }
                            """);

    checkCompletion("ddd", "dddId");
    checkNonExistingCompletionVariants("nnnId");
  }

  public void testFieldFromHasOneAndBelongsTo() {
    addDomain("class Ddd {}");
    addDomain("class Mmm {}");
    addDomain("class Nnn {}");
    addDomain("""
                
                class Xxx {
                    Nnn nnn;
                
                  static hasOne = [d: Ddd]
                  static belongsTo = [mmm: Mmm]
                }
                """);

    configureByController("""
                            
                            class CccController {
                              def index = {
                                new Xxx().get<caret>
                              }
                            }
                            """);

    checkCompletion("getdId", "getMmmId");
  }

  public void testResolve() {
    addDomain("class Ddd {}");
    addDomain("""
                
                class Xxx {
                  Ddd ddd;
                }
                """);

    PsiFile file = addController("""
                                   
                                   class CccController {
                                     def index = {
                                       new Xxx().getDddId()
                                       new Xxx().dddId()
                                     }
                                   }
                                   """);

    GrailsTestCase.checkResolve(file);
  }
}
