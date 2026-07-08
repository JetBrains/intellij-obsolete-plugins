package com.intellij.lang.puppet.resolve;

import com.intellij.lang.puppet.PuppetTestUtil;
import com.intellij.lang.puppet.psi.references.PuppetClassDefinitionReference;
import com.intellij.lang.puppet.psi.references.PuppetClassParameterReference;
import com.intellij.lang.puppet.psi.references.PuppetNamespaceReference;
import com.intellij.testFramework.TestFrameworkUtil;
import junit.framework.TestSuite;

import static com.intellij.lang.puppet.resolve.PuppetResolveTestCase.ResultType.ONE;
import static com.intellij.lang.puppet.resolve.PuppetResolveTestCase.ResultType.ZERO;
import static com.intellij.lang.puppet.resolve.PuppetResolveTestCase.TestType.ALL;
import static com.intellij.lang.puppet.resolve.PuppetResolveTestCase.TestType.CARET;

public class PuppetClassResolveTest extends PuppetResolveTestCase {
  @Override
  protected String getBasePath() {
    return super.getBasePath() + "/class";
  }

  public void testClassTypeParameter() {
    doTest(ALL);
  }

  public void testIncludeClass() {
    doTest(CARET, ONE, PuppetClassDefinitionReference.class);
  }

  public void testInheritClass() {
    doTest(CARET, ONE, PuppetClassDefinitionReference.class);
  }

  public void testNestedNo() {
    doTest(CARET, ZERO, PuppetClassDefinitionReference.class);
  }

  public void testNestedYes() {
    doTest(CARET, ONE, PuppetClassDefinitionReference.class);
  }

  public void testQualifiedParts() {
    doTest(CARET, ONE, PuppetNamespaceReference.class);
  }

  public void testQualifiedPartsRecover() {
    doTest(CARET, ONE, PuppetNamespaceReference.class);
  }

  public void testRootQualified() {
    doTest(CARET, ONE, PuppetClassDefinitionReference.class);
  }

  public void testSimpleLibClassInclude() {
    doTest(CARET, ONE, PuppetClassDefinitionReference.class);
  }

  public void testQualifiedLibClassInclude() {
    doTest(CARET, ONE, PuppetClassDefinitionReference.class);
  }

  /*
  // this needs another approach, can't figure out which one
  public void testConsiderWrittenModule() {
    myFixture.copyDirectoryToProject("writtenlib", "writtenlib");
    myFixture.copyFileToProject("writtenlibInternal.pp", "writtenlib/manifests/test.pp");
    doTest(CARET, ONE, PuppetClassDefinitionReference.class, "../writtenlib/manifests/test.pp", null);
  }
  */

  public void testResourceLike1() {
    doTest(CARET, ONE, PuppetClassDefinitionReference.class);
  }

  public void testResourceLike2() {
    doTest(CARET, ONE, PuppetClassDefinitionReference.class);
  }

  public void testResourceLike3() {
    doTest(CARET, ONE, PuppetClassDefinitionReference.class);
  }

  public void testResourceLikeMulti() {
    doTest(CARET, ONE, PuppetClassDefinitionReference.class);
  }

  public void testResourceLikeQualified1() {
    doTest(CARET, ONE, PuppetClassDefinitionReference.class);
  }

  public void testResourceReference1() {
    doTest(CARET, ONE, PuppetClassDefinitionReference.class);
  }

  public void testResourceLikeParam() {
    doTest(CARET, ONE, PuppetClassParameterReference.class);
  }

  public void testResourceLikeQualifiedParam1() {
    doTest(CARET, ZERO, PuppetClassParameterReference.class);
  }

  public void testResourceLikeQualifiedParam2() {
    doTest(CARET, ONE, PuppetClassParameterReference.class);
  }

  public static TestSuite suite() {
    return TestFrameworkUtil.flattenSuite(PuppetTestUtil.createTestSuiteForVersions(PuppetClassResolveTest.class));
  }
}
