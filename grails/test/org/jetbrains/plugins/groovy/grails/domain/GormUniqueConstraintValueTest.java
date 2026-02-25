// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.groovy.grails.domain;

import org.jetbrains.plugins.groovy.grails.GrailsTestCase;

public class GormUniqueConstraintValueTest extends GrailsTestCase {
  public void testCompletion1() throws Exception {
    addSimpleGroovyFile("""
                          
                          class Zzz {
                            int iii;
                          }
                          """);

    configureByDomain("""
                        
                        class Ddd extends Zzz {
                          String name;
                          String title;
                        
                          static constraints = {
                            name(unique: '<caret>')
                          }
                        }
                        """);

    checkCompletion("title", "iii");
    checkNonExistingCompletionVariants("name", "id", "version");
  }

  public void testCompletion2() {
    configureByDomain("""
                        
                        class Ddd {
                          String name;
                          String title;
                          String zzz;
                          Ddd ddd
                        
                          static constraints = {
                            name(unique: ['<caret>', 'zzz'])
                          }
                        }
                        """);

    checkCompletion("title", "ddd");
    checkNonExistingCompletionVariants("name", "zzz");
  }

  public void testCompletion3() {
    configureByDomain("""
                        
                        class Ddd {
                          String name;
                          String title;
                        
                          static hasMany = [many: String]
                          static hasOne = [one: Ddd]
                        
                          def transientProperty
                        
                          static constraints = {
                            name([unique: ['<caret>', 'zzz']])
                          }
                        }
                        """);

    checkCompletion("title", "one");
    checkNonExistingCompletionVariants("name", "zzz", "transientProperty", "many");
  }

  public void testRename() {
    configureByDomain("""
                        
                        class Ddd {
                          String name;
                          String title<caret>;
                        
                          static constraints = {
                            name([unique: 'title'])
                          }
                        }
                        """);

    myFixture.renameElementAtCaret("ttt");

    myFixture.checkResult("""
                            
                            class Ddd {
                              String name;
                              String ttt;
                            
                              static constraints = {
                                name([unique: 'ttt'])
                              }
                            }
                            """);
  }
}
