/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.intellij.javascript.testFramework;

import com.intellij.lang.javascript.psi.JSFile;
import com.intellij.psi.PsiFile;
import com.intellij.testFramework.TestDataFile;
import com.intellij.testFramework.fixtures.LightPlatformCodeInsightFixtureTestCase;
import com.intellij.util.ObjectUtils;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

public abstract class AbstractJsPsiTestCase extends LightPlatformCodeInsightFixtureTestCase {

  protected void validateJsFile() {
    validateJsFile(getTestName(true));
  }

  private void validateJsFile(final String fileNameWithoutExtension) {
    validateFile(fileNameWithoutExtension + ".js");
  }

  protected void validateFile(@TestDataFile @NonNls String filePath) {
    JSFile jsFile = getJsFile(filePath);
    validateJsFile(jsFile, jsFile.getText());
  }

  @NotNull
  protected JSFile getJsFile(@TestDataFile @NonNls String filePath) {
    PsiFile file = myFixture.configureByFile(filePath);
    JSFile jsFile = ObjectUtils.tryCast(file, JSFile.class);
    if (jsFile == null) {
      fail(JSFile.class + " was expected, but " + (file == null ? "null " : file.getClass()) + " found.");
    }
    return jsFile;
  }

  protected abstract void validateJsFile(JSFile jsFile, String fileText);

}
