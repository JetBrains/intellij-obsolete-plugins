package com.intellij.frameworks.play;

import com.intellij.openapi.fileTypes.FileType;
import com.intellij.play.language.PlayFileType;
import com.intellij.psi.formatter.FormatterTestCase;

import java.io.File;

public class PlayFormatterTest extends FormatterTestCase {
  @Override
  protected String getTestDataPath() {
    return new File(".").getAbsolutePath();
  }

  @Override
  protected String getBasePath() {
    return "src/test/testData/formatter/";
  }

  @Override
  protected String getFileExtension() {
    return "html";
  }

  public void testSimple() throws Exception {
    doTest();
  }

  public void testChildTag() throws Exception {
    doTest();
  }

  @Override
  protected FileType getFileType(String fileName) {
    return PlayFileType.INSTANCE;
  }
}
