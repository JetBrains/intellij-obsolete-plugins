package com.intellij.lang.puppet.resolve;

import com.intellij.lang.puppet.PuppetTestUtil;
import com.intellij.lang.puppet.psi.references.PuppetVariableReference;
import com.intellij.testFramework.TestFrameworkUtil;
import junit.framework.TestSuite;

import static com.intellij.lang.puppet.resolve.PuppetResolveTestCase.ResultType.ONE;
import static com.intellij.lang.puppet.resolve.PuppetResolveTestCase.ResultType.SERIALIZE;
import static com.intellij.lang.puppet.resolve.PuppetResolveTestCase.ResultType.ZERO;
import static com.intellij.lang.puppet.resolve.PuppetResolveTestCase.TestType.ALL;
import static com.intellij.lang.puppet.resolve.PuppetResolveTestCase.TestType.CARET;

public class PuppetVariableResolveTest extends PuppetResolveTestCase {
  @Override
  protected String getBasePath() {
    return super.getBasePath() + "/variable";
  }

  public void testBuiltins() { doTest(ALL);}

  public void testRuby18464() { doTest(ALL);}

  public void testRuby17951() { doTest(ALL);}

  public void testRuby17193() { doTest(ALL);}

  public void testRuby17097() { doTest(ALL);}

  public void testClassNameAndTitle() { doTest(ALL);}

  public void testSimpleVariable() {
    doTest(CARET, ONE, PuppetVariableReference.class);
  }

  public void testComplexVariable() {
    doTest(CARET, SERIALIZE, PuppetVariableReference.class);
  }

  public void testSimpleClassParameter() {
    doTest(CARET, ONE, PuppetVariableReference.class);
  }

  public void testInheritedClassParameter() {
    doTest(CARET, ONE, PuppetVariableReference.class);
  }

  public void testSimpleDefinedResourceParameter() {
    doTest(CARET, ONE, PuppetVariableReference.class);
  }

  public void testVariableInheritance() {
    doTest(CARET, SERIALIZE, PuppetVariableReference.class);
  }

  public void testVariableInheritanceWithFqn() {
    doTest(CARET, SERIALIZE, PuppetVariableReference.class);
  }

  public void testSimpleInterpolatedVariable() {
    doTest(CARET, ONE, PuppetVariableReference.class);
  }

  public void testQualifiedInClass() {
    doTest(CARET, ONE, PuppetVariableReference.class);
  }

  public void testQualifiedInClassFromAnotherClass() {
    doTest(CARET, ONE, PuppetVariableReference.class);
  }

  public void testQualifiedInClassInTheSameClass() {
    doTest(CARET, ONE, PuppetVariableReference.class);
  }

  public void testFullyQualifiedInClass() {
    doTest(CARET, ONE, PuppetVariableReference.class);
  }

  public void testQualifiedInDefinedResource() {
    doTest(CARET, ZERO, PuppetVariableReference.class);
  }

  public void testVariableInResource() {
    doTest(CARET, ZERO, PuppetVariableReference.class);
  }

  public void testVariableInNode() {
    doTest(CARET, SERIALIZE, PuppetVariableReference.class);
  }

  public void testFactsDotDInLib() {
    doTest(ALL, SERIALIZE, PuppetVariableReference.class);
  }

  /**
   * If this test fails, try adding ruby to the classpath
   */
  public void testFactsFromRubyDSL() {
    doTest(ALL, SERIALIZE, PuppetVariableReference.class);
  }

  public void testVariablesAll() {
    doTest(ALL, SERIALIZE, PuppetVariableReference.class);
  }

  public void testTextualDeepNode() {
    myFixture.addFileToProject("manifests/mydecl.pp",
                               "class myclass {" +
                               "  node 'aaa.bbb.ccc' {" +
                               "    $my_deep_var = 1" +
                               "  }" +
                               "}");
    doTest(CARET, ZERO, PuppetVariableReference.class);
  }

  public void testTextualDeepClass() {
    myFixture.addFileToProject("manifests/mydecl.pp",
                               "class myclass {" +
                               "    $my_deep_var = 1" +
                               "  node 'aaa.bbb.ccc' {" +
                               "  }" +
                               "}");
    doTest(CARET, ONE, PuppetVariableReference.class);
  }

  public static TestSuite suite() {
    return TestFrameworkUtil.flattenSuite(PuppetTestUtil.createTestSuiteForVersions(PuppetVariableResolveTest.class));
  }
}
