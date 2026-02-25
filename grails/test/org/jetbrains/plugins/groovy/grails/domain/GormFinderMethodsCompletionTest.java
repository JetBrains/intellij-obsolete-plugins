// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.groovy.grails.domain;

import com.intellij.psi.PsiFile;
import org.jetbrains.plugins.groovy.grails.GrailsTestCase;

public class GormFinderMethodsCompletionTest extends GrailsTestCase {
  @Override
  protected void setUp() throws Exception {
    super.setUp();
    addDomain("""
                
                class Ddd {
                  String aaa
                  String bbb
                }
                """);
  }

  @Override
  protected boolean useGrails14() {
    return true;
  }

  public void testCompletion1() throws Exception {
    configureBySimpleGroovyFile("Ddd.<caret>");
    checkCompletion("findBy", "findAllBy", "countBy", "findOrCreateBy", "findOrSaveBy");
  }

  public void testCompletion2() throws Exception {
    PsiFile file = addSimpleGroovyFile("Ddd.findAllBy<caret>");
    checkCompletionVariants(file, "findAllByAaa", "findAllByBbb", "findAllById", "findAllByVersion");
  }

  public void testCompletion22() throws Exception {
    PsiFile file = addSimpleGroovyFile("Ddd.findOrCreateBy<caret>");
    checkCompletionVariants(file, "findOrCreateByAaa", "findOrCreateByBbb", "findOrCreateById", "findOrCreateByVersion");
  }

  public void testCompletion4() throws Exception {
    configureBySimpleGroovyFile("Ddd.findAllByAaa<caret>");
    checkCompletion("findAllByAaa()", "findAllByAaaAnd", "findAllByAaaOr", "findAllByAaaInList", "findAllByAaaNotInList",
                    "findAllByAaaNotEqual");
    checkNonExistingCompletionVariants("findAllByAaaEqual", "findAllByAaaNotNotEqual");
  }

  public void testCompletion5() throws Exception {
    configureBySimpleGroovyFile("Ddd.findAllByAaaEqual<caret>");
    checkCompletion("findAllByAaaEqual()", "findAllByAaaEqualOrBbb", "findAllByAaaEqualAndBbb");
    checkNonExistingCompletionVariants("findAllByAaaEqualOr", "findAllByAaaEqualAnd");
  }

  public void testCompletion6() throws Exception {
    configureBySimpleGroovyFile("Ddd.countByAaaGreaterThanEqualsAndAaaLessThanEqualsAndId<caret>");
    checkCompletion("countByAaaGreaterThanEqualsAndAaaLessThanEqualsAndId()",
                    "countByAaaGreaterThanEqualsAndAaaLessThanEqualsAndIdNotEqual",
                    "countByAaaGreaterThanEqualsAndAaaLessThanEqualsAndIdInRange",
                    "countByAaaGreaterThanEqualsAndAaaLessThanEqualsAndIdAnd");
    checkNonExistingCompletionVariants("countByAaaGreaterThanEqualsAndAaaLessThanEqualsAndIdOr");
  }

  public void testCompletion7() throws Exception {
    PsiFile file = addSimpleGroovyFile("Ddd.findOrCreateByAaa<caret>");
    checkCompletionVariants(file, "findOrCreateByAaa", "findOrCreateByAaaAndBbb", "findOrCreateByAaaAndId", "findOrCreateByAaaAndVersion");
  }

  public void testCompletion8() throws Exception {
    PsiFile file = addSimpleGroovyFile("Ddd.findOrCreateByAaaEqual<caret>");
    checkCompletionVariants(file, "findOrCreateByAaaEqual", "findOrCreateByAaaEqualAndBbb", "findOrCreateByAaaEqualAndId",
                            "findOrCreateByAaaEqualAndVersion");
  }
}
