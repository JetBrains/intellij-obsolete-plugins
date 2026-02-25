// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.groovy.grails.domain;

import org.jetbrains.plugins.groovy.grails.GrailsTestCase;

public class GrailsFindWhereParameterTest extends GrailsTestCase {
  public void testRename() {
    configureByDomain("""
                        
                        class Ddd {
                          String name<caret>
                        
                          public static def xxx() {
                            Ddd.findAllWhere(name: 'aaa');
                          }
                        }
                        """);
    myFixture.renameElementAtCaret("name111");

    myFixture.checkResult("""
                            
                            class Ddd {
                              String name111
                            
                              public static def xxx() {
                                Ddd.findAllWhere(name111: 'aaa');
                              }
                            }
                            """);
  }
}
