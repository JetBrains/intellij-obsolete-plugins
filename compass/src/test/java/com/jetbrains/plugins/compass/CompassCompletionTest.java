package com.jetbrains.plugins.compass;

import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.testFramework.VfsTestUtil;

import java.io.IOException;

public class CompassCompletionTest extends CompassTestCase {

  public void testCompassFiles() {
    myFixture.testCompletionVariants(getTestName(true) + ".sass", "compass", "compass", "lemonade");
  }

  public void testFilesFromImportPath() throws IOException {
    final VirtualFile extraImportPath = addCompassIncludePath();
    VfsTestUtil.createFile(extraImportPath, "extraFile.sass");
    myFixture.testCompletionVariants(getTestName(true) + ".sass", "compass", "compass", "extraFile", "lemonade");
  }

  public void testCustomFunctions() {
    myFixture.testCompletionVariants(getTestName(true) + ".sass", "-moz", "-ms", "capture-experimental-matrix", "capture-legacy-ie-matrix");
  }

  public void testCompassFunctions() {
    myFixture.testCompletionVariants(getTestName(true) + ".sass", "prefixed", "prefix", "prefixed-for-transition", "standardized_prefix");
  }

  public void testFunctionsFromImportPath() throws IOException {
    final VirtualFile extraImportPath = addCompassIncludePath();
    VfsTestUtil.createFile(extraImportPath, "extraFile.sass",
                           "@function functionFromImportPath1()\n   @return 1\n\n" +
                           "@function functionFromImportPath2()\n   @return 2\n\n");
    myFixture.testCompletionVariants(getTestName(true) + ".sass", "functionFromImportPath1", "functionFromImportPath2");
  }

  public void testCompassVariables() {
    myFixture.testCompletionVariants(getTestName(true) + ".sass", "contrasted-dark-default", "contrasted-light-default",
                                     "contrasted-lightness-threshold");
  }

  public void testVariablesFromImportPath() throws IOException {
    final VirtualFile extraImportPath = addCompassIncludePath();
    VfsTestUtil.createFile(extraImportPath, "extraFile.sass", "$fromImportPath1: 123\n$fromImportPath2: 321");
    myFixture.testCompletionVariants(getTestName(true) + ".sass", "fromImportPath1", "fromImportPath2");
  }

  public void testCompassMixins() {
    myFixture.testCompletionVariants(getTestName(true) + ".sass", "transition-delay", "transition-duration",
                                     "transition-property", "transition-timing-function");
  }

  public void testMixinsFromImportPath() throws IOException {
    final VirtualFile extraImportPath = addCompassIncludePath();
    VfsTestUtil.createFile(extraImportPath, "extraFile.sass",
                           "@mixin fromImportPath1()\n   color: red\n\n" +
                           "@mixin fromImportPath2()\n   color: blue\n\n");
    myFixture.testCompletionVariants(getTestName(true) + ".sass", "fromImportPath1", "fromImportPath2");
  }

  @Override
  protected String getTestDataSubdir() {
    return "completion";
  }
}
