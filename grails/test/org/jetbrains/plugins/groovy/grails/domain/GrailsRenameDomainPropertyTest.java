// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.groovy.grails.domain;

import com.intellij.psi.PsiFile;
import junit.framework.TestCase;
import org.jetbrains.plugins.groovy.grails.GrailsTestCase;

/**
 * This test tests case of issue IDEA-69797 (http://youtrack.jetbrains.net/issue/IDEA-69797)
 */
public class GrailsRenameDomainPropertyTest extends GrailsTestCase {
  public void testRenameDomainProperty() throws Exception {
    PsiFile domainClass = addDomain("""
                                      
                                      class Ddd {
                                        String name;
                                      
                                        public boolean isUnnamed() {
                                          return name == null;
                                        }
                                      }
                                      """);

    configureBySimpleGroovyFile("new Ddd().isUnnamed<caret>()");

    myFixture.renameElementAtCaret("isEmptyName");

    TestCase.assertEquals("""
                            
                            class Ddd {
                              String name;
                            
                              public boolean isEmptyName() {
                                return name == null;
                              }
                            }
                            """, domainClass.getText());
  }
}
