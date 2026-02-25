// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.groovy.grails.tests;

import com.intellij.psi.PsiFile;
import org.jetbrains.plugins.groovy.grails.Grails14TestCase;

public class GrailsMockAnnotationTest extends Grails14TestCase {
  public void testCompletion() {
    addController("class CccController {}");

    PsiFile testFile = myFixture.addFileToProject("test/unit/TttTest.groovy", """
      import grails.test.mixin.*
      @Mock(CccController)
      class TttTest {
        private void xxx() {
          <caret>
        }
      }
      """);

    checkCompletion(testFile, "log", "assertEquals");
  }

  @Override
  protected boolean needJUnit() {
    return true;
  }
}
