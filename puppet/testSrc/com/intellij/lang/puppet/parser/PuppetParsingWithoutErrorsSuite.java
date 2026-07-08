package com.intellij.lang.puppet.parser;

import com.intellij.lang.puppet.PuppetTestUtil;
import com.intellij.lang.puppet.psi.PuppetParserDefinition;
import com.intellij.lang.puppet.settings.PuppetProjectConfiguration;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.PsiErrorElement;
import com.intellij.psi.PsiFile;
import com.intellij.testFramework.ParsingTestCase;
import com.intellij.testFramework.TestDataFile;
import com.intellij.util.ThrowableRunnable;
import junit.framework.AssertionFailedError;
import junit.framework.Test;
import junit.framework.TestSuite;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.junit.Assert;

import java.io.File;
import java.util.List;
import java.util.regex.Pattern;

public class PuppetParsingWithoutErrorsSuite extends TestSuite {
  public PuppetParsingWithoutErrorsSuite() {
    File dataRoot = FileUtil.findFirstThatExist(PuppetTestUtil.getTestDataPath() + "/parser_no_errors");
    Assert.assertNotNull(dataRoot);

    List<File> puppetFiles = FileUtil.findFilesByMask(Pattern.compile(".*\\.pp"), dataRoot);

    for (File f : puppetFiles) {
      String fileName = f.getName();
      String testName = fileName.substring(0, fileName.length() - 3);
      addTest(new PuppetLightParserTest(testName));
    }
  }

  public static Test suite() {
    return new PuppetParsingWithoutErrorsSuite();
  }

  @SuppressWarnings("JUnitTestCaseWithNoTests")
  private static class PuppetLightParserTest extends ParsingTestCase {
    private final String myFileName;

    @SuppressWarnings("JUnitTestCaseWithNonTrivialConstructors")
    PuppetLightParserTest(String fileName) {
      super("parser_no_errors", "pp", true, new PuppetParserDefinition());
      myFileName = fileName;
    }

    @Override
    protected void setUp() throws Exception {
      super.setUp();
      project.registerService(PuppetProjectConfiguration.class);
    }

    @Override
    protected String getTestDataPath() {
      return PuppetTestUtil.getTestDataPath();
    }

    @Override
    protected @NotNull String getTestName(boolean lowercaseFirstLetter) {
      return myFileName;
    }

    @Override
    public String getName() {
      return myFileName;
    }

    @Override
    public void runTestRunnable(@NotNull ThrowableRunnable<Throwable> testRunnable) {
      try {
        setUp();
        doTest(true);
        tearDown();
      }
      catch (Exception e) {
        throw new RuntimeException(e);
      }
    }

    @Override
    protected void checkResult(@NotNull @NonNls @TestDataFile String targetDataName, final @NotNull PsiFile file) {
      file.accept(new PsiElementVisitor() {
        @Override
        public void visitElement(@NotNull PsiElement element) {
          assertNonErrorElement(element);
          element.acceptChildren(this);
        }

        private void assertNonErrorElement(PsiElement element) {
          if (element instanceof PsiErrorElement) {
            throw new AssertionFailedError("Errors while parsing file: " + file.getVirtualFile().getName());
          }
        }
      });
    }
  }

}

