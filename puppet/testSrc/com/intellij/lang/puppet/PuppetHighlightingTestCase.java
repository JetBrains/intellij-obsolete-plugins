package com.intellij.lang.puppet;

import com.intellij.lang.puppet.project.PuppetTestCase;
import org.jetbrains.annotations.NotNull;

public abstract class PuppetHighlightingTestCase extends PuppetTestCase {

  protected @NotNull String getFileExtension() {
    return PuppetFileType.DEFAULT_EXTENSION;
  }


  protected void doTest() {
    myFixture.testHighlighting(getTestName(true) + "." + getFileExtension());
  }
}
