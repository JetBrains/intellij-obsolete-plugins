package com.intellij.lang.puppet.resolve;

import com.intellij.lang.puppet.OnVersion;
import com.intellij.lang.puppet.PuppetLanguage;
import com.intellij.lang.puppet.PuppetTestUtil;
import com.intellij.lang.puppet.psi.references.PuppetNamespaceReference;
import com.intellij.lang.puppet.psi.references.PuppetTypeDefinitionReference;
import com.intellij.lang.puppet.psi.references.PuppetTypeParameterReference;
import com.intellij.testFramework.TestFrameworkUtil;
import junit.framework.TestSuite;

import static com.intellij.lang.puppet.resolve.PuppetResolveTestCase.ResultType.ONE;
import static com.intellij.lang.puppet.resolve.PuppetResolveTestCase.ResultType.SERIALIZE;
import static com.intellij.lang.puppet.resolve.PuppetResolveTestCase.ResultType.ZERO;
import static com.intellij.lang.puppet.resolve.PuppetResolveTestCase.TestType.ALL;
import static com.intellij.lang.puppet.resolve.PuppetResolveTestCase.TestType.CARET;

public class PuppetTypeResolveTest extends PuppetResolveTestCase {
  @Override
  protected String getBasePath() {
    return super.getBasePath() + "/type";
  }

  @OnVersion(PuppetLanguage.Version.PUPPET_4)
  public void testParameterTypes() {
    doTest(ALL);
  }

  @OnVersion(PuppetLanguage.Version.PUPPET_4)
  public void testProducesAndConsumes() {
    doTest(ALL);
  }

  public void testRuby18637() {
    doTest(ALL);
  }

  public void testRuby18703() {
    doTest(ALL);
  }

  public void testTypeInCaseOrSwitch() {
    doTest(ALL);
  }

  public void testMyResourceDecl() {
    doTest(CARET, ONE, PuppetTypeDefinitionReference.class);
  }

  public void testMyVirtualResource() {
    doTest(CARET, ONE, PuppetTypeDefinitionReference.class);
  }

  public void testMyQualifiedResourceNo() {
    doTest(CARET, ZERO, PuppetTypeDefinitionReference.class);
  }

  public void testMyQualifiedResourceYes() {
    doTest(CARET, ONE, PuppetTypeDefinitionReference.class);
  }

  public void testMyResourceOverride() {
    doTest(CARET, ONE, PuppetTypeDefinitionReference.class);
  }

  public void testMyResourceRef() {
    doTest(CARET, ONE, PuppetTypeDefinitionReference.class);
  }

  public void testMyResourceWithoutName() {
    doTest(CARET, ONE, PuppetTypeDefinitionReference.class);
  }

  public void testMyResOverrideParam() {
    doTest(CARET, ONE, PuppetTypeParameterReference.class);
  }

  public void testMyResParam() {
    doTest(CARET, ONE, PuppetTypeParameterReference.class);
  }

  public void testMyResRefParam() {
    doTest(CARET, ONE, PuppetTypeParameterReference.class);
  }

  public void testMyResWithoutNameParam() {
    doTest(CARET, ONE, PuppetTypeParameterReference.class);
  }

  public void testMyQualifiedClassParts() {
    doTest(CARET, ONE, PuppetNamespaceReference.class);
  }

  public void testMyQualifiedClassPartsCapitalized() {
    doTest(CARET, ONE, PuppetNamespaceReference.class);
  }

  public void testMyQualifiedInternal() {
    doTest(ALL);
  }

  public void testMyQualifiedResourceCapitalized() {
    doTest(CARET, ONE, PuppetTypeDefinitionReference.class);
  }

  public void testSimpleLibResourceInstance() {
    doTest(CARET, ONE, PuppetTypeDefinitionReference.class);
  }

  public void testReachingTopScopeResource() {
    doTest(CARET, SERIALIZE, PuppetTypeDefinitionReference.class);
  }

  public static TestSuite suite() {
    return TestFrameworkUtil.flattenSuite(PuppetTestUtil.createTestSuiteForVersions(PuppetTypeResolveTest.class));
  }
}
