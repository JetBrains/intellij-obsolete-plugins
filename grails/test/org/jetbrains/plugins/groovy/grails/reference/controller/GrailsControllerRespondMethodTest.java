// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.groovy.grails.reference.controller;

import org.jetbrains.plugins.groovy.grails.Grails14TestCase;

public class GrailsControllerRespondMethodTest extends Grails14TestCase {
  public void testViewCompletion() {
    addView("aaa.gsp", "");
    addView("bbb.gsp", "");

    configureByController("""
                            class CccController {
                              def foo() = {
                                respond(view: '/<caret>')
                              }
                            }
                            """);

    checkCompletion("aaa", "bbb");
  }
}
