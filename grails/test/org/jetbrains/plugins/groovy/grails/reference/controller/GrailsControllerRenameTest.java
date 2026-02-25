// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.groovy.grails.reference.controller;

import org.jetbrains.plugins.groovy.grails.GrailsTestCase;

public class GrailsControllerRenameTest extends GrailsTestCase {
  public void testRename() {
    addController("""
                    class ZzzController {
                      def aaa = {}
                    }
                    """);

    configureByController("""
                            class CccController {
                              def index = {
                                link(controller: 'zzz', action: 'aaa')
                                link(controller: 'ccc<caret>')
                              }
                            }
                            """);

    myFixture.renameElementAtCaret("CcController");

    myFixture.checkResult("""
                            class CcController {
                              def index = {
                                link(controller: 'zzz', action: 'aaa')
                                link(controller: 'cc<caret>')
                              }
                            }
                            """);
  }
}
