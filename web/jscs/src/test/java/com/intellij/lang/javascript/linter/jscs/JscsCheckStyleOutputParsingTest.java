package com.intellij.lang.javascript.linter.jscs;

import com.intellij.execution.process.ProcessEvent;
import com.intellij.execution.process.ProcessOutputTypes;
import com.intellij.lang.javascript.linter.JSLinterError;
import com.intellij.lang.javascript.linter.JSLinterErrorBase;
import com.intellij.xdebugger.DefaultDebugProcessHandler;
import org.junit.Assert;
import org.junit.Test;

public class JscsCheckStyleOutputParsingTest {
  @Test
  public void testSomeExample() {
    final String input = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                         "<checkstyle version=\"4.3\">\n" +
                         "    <file name=\"D:\\testProjects\\ws1\\tets\\first.js\">\n" +
                         "        <error line=\"1\" column=\"1\" severity=\"error\" message=\"requireLeftStickedOperators: The requireLeftStickedOperators rule is no longer supported.\n" +
                         "Please use the following rules instead:\n" +
                         "\n" +
                         "disallowSpaceBeforeBinaryOperators\n" +
                         "disallowSpaceBeforePostfixUnaryOperators\n" +
                         "disallowSpacesInConditionalExpression\" source=\"jscs\" />\n" +
                         "        <error line=\"1\" column=\"1\" severity=\"error\" message=\"disallowLeftStickedOperators: The disallowLeftStickedOperators rule is no longer supported.\n" +
                         "Please use the following rules instead:\n" +
                         "\n" +
                         "requireSpaceBeforeBinaryOperators\n" +
                         "requireSpaceBeforePostfixUnaryOperators\n" +
                         "requireSpacesInConditionalExpression\" source=\"jscs\" />\n" +
                         "        <error line=\"1\" column=\"1\" severity=\"error\" message=\"requireRightStickedOperators: The requireRightStickedOperators rule is no longer supported.\n" +
                         "Please use the following rules instead:\n" +
                         "\n" +
                         "disallowSpaceAfterBinaryOperators\n" +
                         "disallowSpaceAfterPrefixUnaryOperators\n" +
                         "disallowSpacesInConditionalExpression\" source=\"jscs\" />\n" +
                         "        <error line=\"1\" column=\"1\" severity=\"error\" message=\"disallowRightStickedOperators: The disallowRightStickedOperators rule is no longer supported.\n" +
                         "Please use the following rules instead:\n" +
                         "\n" +
                         "requireSpaceAfterBinaryOperators\n" +
                         "requireSpaceAfterPrefixUnaryOperators\n" +
                         "requireSpacesInConditionalExpression\" source=\"jscs\" />\n" +
                         "        <error line=\"1\" column=\"4\" severity=\"error\" message=\"validateLineBreaks: Invalid line break\" source=\"jscs\" />\n" +
                         "        <error line=\"5\" column=\"5\" severity=\"error\" message=\"requireCurlyBraces: If statement without curly braces\" source=\"jscs\" />\n" +
                         "    </file>\n" +
                         "</checkstyle>";

    final DefaultDebugProcessHandler processHandler = new DefaultDebugProcessHandler();
    final JscsCheckStyleOutputFormatParser parser = new JscsCheckStyleOutputFormatParser("test");
    final String[] lines = input.split("\n");
    for (String line : lines) {
      parser.onTextAvailable(new ProcessEvent(processHandler, line), ProcessOutputTypes.STDOUT);
    }
    parser.process();

    final JSLinterErrorBase error = parser.getGlobalError();
    Assert.assertNull(error);

    final JscsObsoleteRulesWarning warning = parser.getObsoleteRulesWarning();
    Assert.assertNotNull(warning);

    final java.util.List<JSLinterError> errors = parser.getErrors();
    Assert.assertEquals(2, errors.size());
  }
}
