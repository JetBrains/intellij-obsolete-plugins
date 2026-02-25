// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.groovy.grails.domain;

import org.jetbrains.plugins.groovy.grails.GrailsTestCase;

public class GormGetPersistentValueReturnTypeTest extends GrailsTestCase {
  public void testCompletion() throws Exception {
    addDomain("""
                
                class Ddd {
                  String name
                }
                """);

    configureBySimpleGroovyFile("new Ddd().getPersistentValue('name').<caret>");
    checkCompletion("substring", "length", "charAt");
  }
}
