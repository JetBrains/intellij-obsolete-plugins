// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.groovy.grails.domain;

import org.jetbrains.plugins.groovy.grails.GrailsTestCase;

public class GormDomainFinderMethodTest extends GrailsTestCase {
  public void testCompletionMethod() throws Exception {
    addDomain("""
                
                class Product {
                
                  String name
                  String date
                  String quality
                  String weight
                
                  public String getSize() {
                    return "asda"
                  }
                
                  public void setSize(String size) {
                
                  }
                
                  int transientField1;
                  String transientField2;
                
                  static transients = ['transientField1', "transientField2"]
                }
                """);

    configureBySimpleGroovyFile("Product.findAllByDateAndNameIlik<caret>");
    assertTrue(myFixture.completeBasic().length >= 1);
    myFixture.type("\n");

    myFixture.checkResult("Product.findAllByDateAndNameIlike(<caret>)");
  }
}
