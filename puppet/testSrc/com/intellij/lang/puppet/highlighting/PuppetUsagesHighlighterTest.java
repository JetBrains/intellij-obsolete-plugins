package com.intellij.lang.puppet.highlighting;

import com.intellij.lang.puppet.PuppetFileType;
import com.intellij.lang.puppet.PuppetTestUtil;
import com.intellij.lang.puppet.project.PuppetTestCase;
import com.intellij.openapi.editor.RangeMarker;
import com.intellij.openapi.editor.colors.EditorColors;
import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.openapi.editor.colors.EditorColorsScheme;
import com.intellij.openapi.editor.markup.RangeHighlighter;
import com.intellij.openapi.editor.markup.TextAttributes;
import com.intellij.openapi.util.TextRange;
import com.intellij.testFramework.TestFrameworkUtil;
import com.intellij.util.containers.ContainerUtil;
import junit.framework.TestSuite;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

public class PuppetUsagesHighlighterTest extends PuppetTestCase {
  private TextAttributes myReadAttributes;
  private TextAttributes myWriteAttributes;

  @Override
  protected String getBasePath() {
    return "/usageshighlighting";
  }

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    EditorColorsScheme scheme = EditorColorsManager.getInstance().getGlobalScheme();
    myReadAttributes = scheme.getAttributes(EditorColors.SEARCH_RESULT_ATTRIBUTES);
    myWriteAttributes = scheme.getAttributes(EditorColors.WRITE_SEARCH_RESULT_ATTRIBUTES);
  }

  public void testNamespaceUsage() {doTest();}

  public void testClassDeclaration() {doTest();}

  public void testTypeDeclaration() {doTest();}

  public void testRuby18959() { doTest();}

  public void testNamespaceDeclaration() {doTest();}

  private void doTest() {
    // fixme we could extract this
    String testName = getTestName(true);
    String testFileName = testName + "." + PuppetFileType.DEFAULT_EXTENSION;
    String testAnswerFileName = "answers/" + testName + ".txt";

    String actualData = getSerializedResults(testFileName);
    assertSameLinesWithFile(getTestDataPath() + "/" + testAnswerFileName, actualData);
  }

  private String getSerializedResults(@NotNull String testFileName) {
    StringBuilder result = new StringBuilder();

    List<RangeHighlighter> highlighters = Arrays.asList(myFixture.testHighlightUsages(testFileName));
    ContainerUtil.sort(highlighters, Comparator.comparingInt(RangeMarker::getStartOffset));

    for (RangeHighlighter highlighter : highlighters) {
      String usageType;
      TextAttributes attributes = highlighter.getTextAttributes(null);

      if (attributes == null) {
        continue;
      }
      else if (attributes.equals(myReadAttributes)) {
        usageType = "read usage";
      }
      else if (attributes.equals(myWriteAttributes)) {
        usageType = "write usage";
      }
      else {
        continue;
      }

      TextRange range = highlighter.getTextRange();
      CharSequence text = highlighter.getDocument().getCharsSequence();
      result
        .append(range)
        .append(" - '")
        .append(range.subSequence(text))
        .append("': ")
        .append(usageType)
        .append("\n")
      ;
    }

    return result.toString();
  }

  public static TestSuite suite() {
    return TestFrameworkUtil.flattenSuite(PuppetTestUtil.createTestSuiteForVersions(PuppetUsagesHighlighterTest.class));
  }
}

