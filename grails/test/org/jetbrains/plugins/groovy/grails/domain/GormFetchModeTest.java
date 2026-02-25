// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.groovy.grails.domain;

import com.intellij.psi.PsiFile;
import org.jetbrains.plugins.groovy.grails.GrailsTestCase;

public class GormFetchModeTest extends GrailsTestCase {
  public void testCompletion() {
    PsiFile file = addDomain("""
                               
                               class Ddd {
                                 static <caret>
                               
                               }
                               """);

    checkCompletion(file, "fetchMode");
  }

  public void testCompletionFieldName1() {
    PsiFile file = addDomain("""
                               
                               class Ddd {
                                  String name;
                               
                                 static hasMany = [mirrors: Mirror]
                                 static fetchMode = [<caret>: 'eager']
                               }
                               """);

    checkCompletion(file, "mirrors");
    checkNonExistingCompletionVariants("name", "id");
  }

  public void testCompletionFieldName2() {
    PsiFile file = addDomain("""
                               
                               class Ddd {
                                 String name;
                                 List mirrors;
                               
                                 static hasMany = [mirrors: Mirror]
                                 static fetchMode = [<caret>, ]
                               }
                               """);

    checkCompletion(file, "mirrors");
    checkNonExistingCompletionVariants("name", "id");
  }

  public void testCompletionFieldName3() {
    PsiFile file = addDomain("""
                               
                               class Ddd {
                                 String name;
                               
                                 static hasMany = [mirrors: Mirror]
                                 static fetchMode = [xxx: <caret>]
                               }
                               """);

    checkCompletion(file);
    checkNonExistingCompletionVariants("mirrors");
  }

  public void testRename() {
    configureByDomain("""
                        
                        class DownloadFile {
                          String title
                          List mirrors
                          Download download
                        
                          static hasMany = [mirrors: Mirror]
                          static fetchMode = [mirrors<caret>: 'eager']
                        }
                        """);

    myFixture.renameElementAtCaret("mmm");

    myFixture.checkResult("""
                            
                            class DownloadFile {
                              String title
                              List mmm
                              Download download
                            
                              static hasMany = [mmm: Mirror]
                              static fetchMode = [mmm: 'eager']
                            }
                            """);
  }
}
