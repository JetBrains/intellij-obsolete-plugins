// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.groovy.grails.domain;

import org.jetbrains.plugins.groovy.grails.Grails14TestCase;

public class NamedArgumentsInFindOrCreateMethodTest extends Grails14TestCase {
  public void testCompletion() {
    addDomain("""
                
                class Ddd {
                  String name;
                  String zzz;
                  int iii;
                }
                """);
    configureByController("""
                            
                            class CccController {
                              def index = {
                                Ddd.findOrCreateWhere(name: "Name", <caret>)
                              }
                            }
                            """);

    checkCompletion("zzz", "iii");
    checkNonExistingCompletionVariants("name");
  }

  public void testRename() {
    addDomain("""
                
                class Ddd {
                  String name;
                }
                """);
    configureByController("""
                            
                            class CccController {
                              def index = {
                                Ddd.findOrCreateWhere(name<caret>: "Name")
                              }
                            }
                            """);

    myFixture.renameElementAtCaret("firstName");

    myFixture.checkResult("""
                            
                            class CccController {
                              def index = {
                                Ddd.findOrCreateWhere(firstName: "Name")
                              }
                            }
                            """);
  }
}
