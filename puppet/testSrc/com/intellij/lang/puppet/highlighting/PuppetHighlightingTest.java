package com.intellij.lang.puppet.highlighting;

import com.intellij.lang.puppet.OnVersion;
import com.intellij.lang.puppet.PuppetHighlightingTestCase;
import com.intellij.lang.puppet.PuppetLanguage;
import com.intellij.lang.puppet.PuppetTestUtil;
import com.intellij.testFramework.TestFrameworkUtil;
import junit.framework.TestSuite;

public class PuppetHighlightingTest extends PuppetHighlightingTestCase {
  @OnVersion(PuppetLanguage.Version.PUPPET_4)
  public void testResourceInstanceFallback() {doTest();}
  public void testSimple(){doTest();}
  public void testError() {doTest();}
  public void testClassParameters() {doTest();}
  public void testClassNamespace() {doTest();}
  public void testClassNoName() {doTest();}
  public void testRegexp() {doTest();}
  public void testResourceRef() {doTest();}
  public void testVirtualResource() {doTest();}
  public void testHypen() {doTest();}
  public void testNode() {doTest();}
  public void testEndComma() {doTest();}
  public void testIfElse() {doTest();}
  public void testNamespacedInheritance() {doTest();}
  public void testSelector() {doTest();}
  public void testHashAccess() {doTest();}
  public void testClassResourceLikeDef() {doTest();}
  public void testArrayAssignment() {doTest();}
  public void testInclude() {doTest();}
  public void testMultiRequires() {doTest();}
  public void testMultiResources() {doTest();}
  public void testNamespacedDefine() {doTest();}
  public void testHashesInAssignment() {doTest();}
  public void testNamespacedVars() {doTest();}
  public void testIn() {doTest();}
  public void testFunctionNoBraces() {doTest();}
  public void testAssignment() {doTest();}
  public void testNamespacedReference() {doTest();}
  public void testMultiCase() {doTest();}
  public void testCollection() {doTest();}
  public void testNameFormat() {doTest();}
  public void testClassInNode() {doTest();}
  public void testHashEndingComma(){doTest();}

  public void testRuby12592() {doTest();}
  public void testRuby12467() {doTest();}
  public void testRuby12547() {doTest();}
  public void testRuby12545() {doTest();}
  public void testRuby12544() {doTest();}
  public void testRuby12543() {doTest();}
  public void testRuby12523() {doTest();}
  public void testRuby12520() {doTest();}
  public void testRuby12566() {doTest();}
  public void testRuby12575() {doTest();}
  public void testRuby12440() {doTest();}
  public void testRuby12583() {doTest();}
  public void testRuby12415() {doTest();}
  public void testRuby12585() {doTest();}
  public void testRuby12587() {doTest();}
  public void testRuby12627() {doTest();}
  public void testRuby12629() {doTest();}
  public void testRuby13040() {doTest();}
  public void testRuby13385() {doTest();}
  public void testRuby13393() {doTest();}
  public void testRuby13255() {doTest();}
  public void testRuby13563() {doTest();}
  public void testRuby13561() {doTest();}
  public void testRuby13562() {doTest();}
  public void testRuby13559() {doTest();}
  public void testRuby13646() {doTest();}

  public void testRuby14087case1() {doTest();}
  public void testRuby14087case2() {doTest();}

  @Override
  protected String getBasePath() {
    return "/highlighting";
  }

  public static TestSuite suite() {
    return TestFrameworkUtil.flattenSuite(PuppetTestUtil.createTestSuiteForVersions(PuppetHighlightingTest.class));
  }
}
