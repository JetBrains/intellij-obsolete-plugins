// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.groovy.grails.reference.controller;

import com.intellij.openapi.util.registry.Registry;
import org.jetbrains.plugins.groovy.grails.GrailsTestCase;

public class GrailsRenderMethodTest extends GrailsTestCase {
  public void testEncoding() {
    configureByController("""
                            class CccController {
                              def index = {
                                render(encoding: '<caret>')
                              }
                            }
                            """);
    checkCompletion("UTF-8", "KOI8-R");
  }

  public void testContentType() {
    Registry.get("ide.completion.variant.limit").setValue(10000, getTestRootDisposable());

    configureByController("""
                            class CccController {
                              def index = {
                                render(contentType: '<caret>')
                              }
                            }
                            """);
    checkCompletion("text/plain", "text/html");
  }
}
