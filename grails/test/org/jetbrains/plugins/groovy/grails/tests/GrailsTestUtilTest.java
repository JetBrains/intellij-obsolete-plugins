// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.groovy.grails.tests;

import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassOwner;
import com.intellij.psi.PsiFile;
import com.intellij.testFramework.UsefulTestCase;
import junit.framework.TestCase;
import org.jetbrains.plugins.grails.tests.GrailsTestUtils;
import org.jetbrains.plugins.groovy.grails.Grails14TestCase;

import java.util.Collection;

public class GrailsTestUtilTest extends Grails14TestCase {
  public void testTestUtil() {
    PsiFile domain = addDomain("""
                                 package eee;
                                 class Ddd {}
                                 """);
    myFixture.addFileToProject("test/unit/xxx/DddTest.groovy", "package xxx;\n class DddTest {}");
    myFixture.addFileToProject("test/integration/xxx/yyy/DddIntegrationTest.groovy", "package xxx.yyy;\n class DddIntegrationTest {}");
    myFixture.addFileToProject("test/integration/fff/Fff.groovy", """
      package fff;
      
      import grails.test.mixin.TestFor;
      
      @TestFor(eee.Ddd)
      class Fff {
      
      }
      """);

    PsiClass domainClass = ((PsiClassOwner)domain).getClasses()[0];

    Collection<PsiClass> tests = GrailsTestUtils.getTestsForArtifact(domainClass, true);
    UsefulTestCase.assertSize(3, tests);

    for (PsiClass t : tests) {
      TestCase.assertEquals(domainClass, GrailsTestUtils.getTestedClass(t));
    }
  }
}
