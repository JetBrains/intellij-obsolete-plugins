// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.groovy.grails.reference.controller;

import org.jetbrains.plugins.groovy.grails.GrailsTestCase;

public class GrailsHttpResponseTest extends GrailsTestCase {
  @Override
  protected boolean needServletApi() {
    return true;
  }

  @Override
  protected boolean useGrails14() {
    return true;
  }

  public void testCompletion() {
    configureByController("""
                            class CccController {
                              def index = {
                                response.<caret>
                              }
                            }
                            """);

    checkCompletion("format", "getMimeTypes");
  }
}
