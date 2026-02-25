// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.groovy.grails.domain;

import org.jetbrains.plugins.groovy.codeInspection.assignment.GroovyAssignabilityCheckInspection;
import org.jetbrains.plugins.groovy.grails.GrailsTestCase;

public class GormHasManyRelationTypeTest extends GrailsTestCase {
  public void testRelationType() throws Exception {
    addDomain("""
                
                class A {
                    List<C> cs
                
                    static hasMany = [cs: C]
                }
                """);

    addDomain("class B extends A {}");
    addDomain("class C { }");

    myFixture.enableInspections(GroovyAssignabilityCheckInspection.class);

    configureBySimpleGroovyFile("""
                                  
                                  class Test {
                                  
                                    Test() {
                                      xxx(new B().cs)
                                      xxx(new A().cs)
                                  
                                      Set<C> s = aaa;
                                      xxx<warning>(s)</warning>
                                    }
                                  
                                    void xxx(List<C> param) {
                                  
                                    }
                                  }
                                  """);

    myFixture.checkHighlighting(true, false, true);
  }
}
