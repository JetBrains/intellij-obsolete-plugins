package com.intellij.lang.puppet.rename;

import com.intellij.idea.IJIgnore;
import com.intellij.lang.puppet.OnVersion;
import com.intellij.lang.puppet.PuppetFileType;
import com.intellij.lang.puppet.PuppetLanguage;
import com.intellij.lang.puppet.PuppetTestUtil;
import com.intellij.lang.puppet.project.PuppetTestCase;
import com.intellij.testFramework.TestFrameworkUtil;
import junit.framework.TestSuite;
import org.jetbrains.annotations.NotNull;

@IJIgnore(issue = "RUBY-34992")
public class PuppetRenameTest extends PuppetTestCase {

  @Override
  protected String getBasePath() {
    return "rename";
  }

  public void testVariableRename() {
    doTest("newName");
  }

  public void testClassRename() {
    doTest();
  }

  public void testNamespaceRename() {
    doTest();
  }

  public void testTypeRename() {
    doTest();
  }

  public void testSynonimsRenameSynonim() {doTest("newname");}

  public void testSynonimsRenameUsage() {doTest("newname");}

  public void testSynonimsRenameTarget() {doTest("newname");}

  public void testSmartResourceInstanceRename() {
    doTest("/smart/name");
  }

  public void testResourceInstanceRename() {
    doTest("newName");
  }

  public void testSmartResourceInstanceRenameWithQuote() {
    doTest("resource'name");
  }

  public void testSmartResourceInstanceRenameWithDoubleQuote() {
    doTest("resource\"name");
  }

  public void testSmartResourceInstanceRenameWithEscapedQuote() {
    doTest("resource\\'name");
  }

  public void testBarewordResourceInstanceRename() {
    doTest("newName");
  }

  @OnVersion(PuppetLanguage.Version.PUPPET_4)
  public void testFunctionRename() {
    doTest();
  }

  private void doTest() {
    doTest("NewName");
  }

  private void doTest(@NotNull String newName) {
    String testName = getTestName(true);
    String testFileName = testName + "." + PuppetFileType.DEFAULT_EXTENSION;
    String testAnswerFileName = testName + ".txt";

    myFixture.testRename(testFileName, testAnswerFileName, newName);
  }

  public static TestSuite suite() {
    return TestFrameworkUtil.flattenSuite(PuppetTestUtil.createTestSuiteForVersions(PuppetRenameTest.class));
  }
}
