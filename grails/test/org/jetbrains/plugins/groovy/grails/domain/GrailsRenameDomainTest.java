// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.groovy.grails.domain;

import org.jetbrains.plugins.groovy.grails.GrailsTestCase;

public class GrailsRenameDomainTest extends GrailsTestCase {
  public void testRename() {
    configureByDomain("""
                        
                        class Ddd {
                          String name;
                          String ttt;
                        
                          static transients = ['ttt']
                        
                          {
                            Ddd<caret>.name = null;
                          }
                        
                          static constraints = {
                            name(nullable: false)
                          }
                        }
                        """);

    myFixture.renameElementAtCaret("Ddd123");

    myFixture.checkResult("""
                            
                            class Ddd123 {
                              String name;
                              String ttt;
                            
                              static transients = ['ttt']
                            
                              {
                                Ddd123.name = null;
                              }
                            
                              static constraints = {
                                name(nullable: false)
                              }
                            }
                            """);
  }
}
