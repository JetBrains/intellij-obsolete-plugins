// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.groovy.grails.completion;

import com.intellij.testFramework.builders.JavaModuleFixtureBuilder;
import org.jetbrains.plugins.groovy.completion.CompletionTestBase;
import org.jetbrains.plugins.groovy.grails.GrailsTestUtil;

import static org.jetbrains.plugins.groovy.grails.GrailsTestUtil.getTestRootPath;

public class ControllerReferenceCompletionTest extends CompletionTestBase {

  public void testContr1Controller() { doTest("grails-app/controllers"); }
  public void testContr2Controller() { doTest("grails-app/controllers"); }
  public void testContr3Controller() { doTest("grails-app/controllers"); }

  @Override
  protected String getTestDataPath() {
    return getTestRootPath("/testdata/grails/oldCompletion/controllers");
  }

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    GrailsTestUtil.createGrailsApplication(myFixture);
  }

  @Override
  protected void tuneFixture(JavaModuleFixtureBuilder moduleBuilder) {
    moduleBuilder.addLibraryJars("GRAILS", GrailsTestUtil.getMockGrails11LibraryHome(), "/dist/grails-web-1.3.1.jar");
  }
}
