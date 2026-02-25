// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.groovy.grails;

import com.intellij.openapi.editor.markup.RangeHighlighter;
import com.intellij.testFramework.UsefulTestCase;
import com.intellij.usageView.UsageInfo;

import java.util.Collection;

public class GrailsLogVariableTest extends GrailsTestCase {
  public void testHighlightingLogUsage() {
    addController("""
                    class CccController {
                      def index = {
                        log<caret>.error()
                        log.debug()
                        log.warning()
                      }
                    }
                    """);
    RangeHighlighter[] res = myFixture.testHighlightUsages("grails-app/controllers/CccController.groovy");

    UsefulTestCase.assertSize(3, res);
  }

  public void testFindUsages() {
    addDomain("""
                class Ddd {
                  String name;
                
                  void xxx() {
                     log.info()
                  }
                
                  static {
                    log.debug()
                  }
                }
                """);

    configureByController("""
                            
                            class CccController {
                              def index = {
                                log<caret>.error()
                                log.debug()
                            
                              }
                            
                              static {
                                log.warning()
                              }
                            }
                            """);

    Collection<UsageInfo> res = myFixture.findUsages(myFixture.getElementAtCaret());

    UsefulTestCase.assertSize(3, res);
  }
}
