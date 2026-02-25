// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package org.jetbrains.plugins.groovy.grails.completion;

import com.intellij.testFramework.builders.JavaModuleFixtureBuilder;
import org.jetbrains.plugins.groovy.completion.CompletionTestBase;
import org.jetbrains.plugins.groovy.grails.GrailsTestUtil;
import org.jetbrains.plugins.groovy.util.TestUtils;

import static org.jetbrains.plugins.groovy.grails.GrailsTestUtil.getTestRootPath;

public class GspTagReferenceCompletionTest extends CompletionTestBase {

  @Override
  protected String getTestDataPath() {
    return getTestRootPath("/testdata/grails/oldCompletion/gsp/");
  }

  @Override
  protected String getExtension() {
    return "gsp";
  }

  @Override
  protected void tuneFixture(JavaModuleFixtureBuilder fixtureBuilder) {
    fixtureBuilder.addLibraryJars("GRAILS", GrailsTestUtil.getMockGrails11LibraryHome(), "/dist/grails-web-1.3.1.jar");
    fixtureBuilder.addLibrary("GROOVY", GrailsTestUtil.getMockGrailsLibraryHome() + '/' + TestUtils.GROOVY_JAR);
    fixtureBuilder.addLibraryJars("Grails", GrailsTestUtil.getMockGrails11LibraryHome(), "/dist/grails-core-1.3.1.jar");

    String path = getTestRootPath("/testdata/mockTagLib");
    fixtureBuilder.addContentRoot(path).addSourceRoot("");
  }

  public void testAttr1() { doTest(); }
  public void testCustom1() { doTest(); }
  public void testCustomNamespacePrefix() { doTest(); }
  public void testDirname() { doTest(); }
  public void testG1() { doTest(); }
  public void testGet1() { doTest(); }
  public void testGroo1() { doTest(); }
  public void testGroo2() { doTest(); }
  public void testGroo3() { doTest(); }
  public void testGroo4() { doTest(); }
  public void testGroo5() { doTest(); }
  public void testHtml1() { doTest(); }
  public void testHtmlWithDoctype() { doTest(); }
  public void testLink() { doTest(); }
  public void testLink2() { doTest(); }
  public void testMy1() { doTest(); }
  public void testPackageTagLib1() { doTest(); }
  public void testPackageTagLib2() { doTest(); }
  public void testTail1() { doTest(); }
  public void testTail2() { doTest(); }
  public void testTail3() { doTest(); }
  public void testTail4() { doTest(); }
  public void testExcludeNotATagGroovy() { doTest(); }
  public void testExcludeNotATagHTML() { doTest(); }
  public void testEndTagTest1() { doTest(); }
  public void testEndTagTest2() { doTest(); }
  public void testEndTagTest3() { doTest(); }
  public void testEndTagTest4() { doTest(); }
  public void testEndTagTest5() { doTest(); }

  public void testNonStatic() { doTest(); }
}
