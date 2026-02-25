// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.groovy.grails.domain;

import org.jetbrains.plugins.groovy.grails.Grails14TestCase;

public class GormPropertyAccessTest extends Grails14TestCase {
  public void testGetAll() throws Exception {
    addDomain("class Ddd { String firstName; String lastName; }");

    configureBySimpleGroovyFile("""
                                  
                                  def x = Ddd.all
                                  x*.<caret>
                                  """);

    checkCompletion("firstName", "lastName");
  }
}
