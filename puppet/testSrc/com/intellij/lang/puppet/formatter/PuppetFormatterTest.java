package com.intellij.lang.puppet.formatter;

import com.intellij.lang.puppet.PuppetFileType;
import com.intellij.lang.puppet.PuppetLanguage;
import com.intellij.lang.puppet.PuppetTestUtil;
import com.intellij.lang.puppet.settings.PuppetProjectConfiguration;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.CharsetToolkit;
import com.intellij.psi.codeStyle.CodeStyleManager;
import com.intellij.testFramework.LightPlatformCodeInsightTestCase;
import com.intellij.testFramework.UsefulTestCase;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;

import static com.intellij.lang.puppet.PuppetLanguage.Version.PUPPET_4;

public class PuppetFormatterTest extends LightPlatformCodeInsightTestCase {

  @Override
  protected @NotNull String getTestDataPath() {
    return PuppetTestUtil.getTestDataPath() + "/formatter/";
  }

  public void testFunctionWithReturnValue() {doTest(PUPPET_4);}

  public void testRuby17069() {doTest();}

  public void testSpacingBuilder() {doTest(PUPPET_4);}

  public void testNewLinesWithHeredoc() {doTest(PUPPET_4);}

  public void testRuby18907() {doTest();}

  public void testRuby19158() {doTest(PUPPET_4);}

  public void testFunctionStatements() {
    doTest();
  }

  public void testResourceInstances() {
    doTest();
  }

  public void testCollections() {
    doTest();
  }

  public void testWrapWithHeredocs() {
    doTest(PUPPET_4);
  }

  public void testClassLikeResource() {
    doTest();
  }

  public void testVariousExpr() {
    doTest();
  }

  public void testAnonymousBlocks() {
    doTest(PUPPET_4);
  }

  public void testConditionals() {
    doTest();
  }

  public void testDefinitions() {
    doTest();
  }

  public void testChainedCalls() {
    doTest(PUPPET_4);
  }

  public void testComments1() {
    doTest();
  }

  public void testHashRockets() {
    doTest();
  }

  public void testRuby17191() {
    doTest(PUPPET_4);
  }

  private void setVersion(@NotNull PuppetLanguage.Version version) {
    PuppetProjectConfiguration.getInstance(getProject()).setLanguageVersion(version);
  }

  @Override
  protected void tearDown() throws Exception {
    try {
      setVersion(PuppetLanguage.Version.PUPPET_3);
    }
    catch (Throwable e) {
      addSuppressedException(e);
    }
    finally {
      super.tearDown();
    }
  }

  private void doTest() {
    doTest(PuppetLanguage.Version.PUPPET_3);
    doTest(PUPPET_4);
  }

  private void doTest(@NotNull PuppetLanguage.Version version) {
    setVersion(version);
    configureByFile();
    WriteCommandAction.runWriteCommandAction(
      getProject(),
      () -> {
        CodeStyleManager codeStyleManager = CodeStyleManager.getInstance(getProject());
        codeStyleManager.reformat(getFile());
      }
    );
    checkResultByFile();
  }

  protected void checkResultByFile() {
    String resultsFile = getTestDataPath() + getTestName(true) + "_after.code";
    UsefulTestCase.assertSameLinesWithFile(resultsFile, getFile().getText());
  }

  protected void configureByFile() {
    try {
      String fileNameWithoutExt = getTestName(true);
      String fullPathWithoutExt = getTestDataPath() + fileNameWithoutExt;
      String fileContent = FileUtil.loadFile(new File(fullPathWithoutExt + ".code"), CharsetToolkit.UTF8, true);
      configureFromFileText(fileNameWithoutExt + "." + PuppetFileType.DEFAULT_EXTENSION, fileContent);
    }
    catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
