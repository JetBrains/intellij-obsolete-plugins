package com.intellij.lang.javascript.linter.jscs;

import com.intellij.execution.process.ProcessAdapter;
import com.intellij.execution.process.ProcessEvent;
import com.intellij.execution.process.ProcessOutputTypes;
import com.intellij.lang.javascript.linter.JSLinterError;
import com.intellij.lang.javascript.linter.JSLinterErrorBase;
import com.intellij.lang.javascript.linter.jscs.config.JscsOption;
import com.intellij.lang.javascript.linter.jscs.config.JscsTypeError;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.xml.NanoXmlUtil;
import net.n3.nanoxml.IXMLElement;
import net.n3.nanoxml.StdXMLBuilder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

/**
 * @author by Irina.Chernushina on 9/23/2014.
 */
public class JscsCheckStyleOutputFormatParser extends ProcessAdapter {
  private static final Logger LOG = Logger.getInstance(JscsConfiguration.LOG_CATEGORY);
  private final static String UNSUPPORTED_RULE = "Unsupported rule:";
  private final static String RULE_IS_NO_LONGER_SUPPORTED = "rule is no longer supported";
  private final static String PLEASE_USE_OTHER = "Please use the following rules instead";
  private final static String NO_FILE_FOUND = "No configuration found. Add a .jscsrc file to your project root or use the -c option.";
  private final static String NO_FILE_FOUND_CORRECTED = "JSCS: No configuration found.";

  private final List<JSLinterError> myErrors;
  private List<String> myObsoleteRulesUsed;
  private List<String> myUnsupportedRulesUsed;
  private final StringBuilder myGlobalError;
  private JSLinterErrorBase myParsedGlobalError;
  private final StringBuilder myText;
  private final String myTmpFile;

  public JscsCheckStyleOutputFormatParser(String actualCodeFile) {
    myTmpFile = actualCodeFile;
    myErrors = new ArrayList<>();
    myGlobalError = new StringBuilder();
    myText = new StringBuilder();
  }

  @Override
  public void onTextAvailable(@NotNull ProcessEvent event, @NotNull Key outputType) {
    final String text = event.getText().trim();
    if (outputType == ProcessOutputTypes.STDERR) {
      final String textToAdd = NO_FILE_FOUND.equals(text) ? NO_FILE_FOUND_CORRECTED : text;
      myGlobalError.append(textToAdd).append("\n");
    } else if (outputType == ProcessOutputTypes.STDOUT) {
      myText.append(text).append("\n");
    }
  }

  public void process() {
    final String text = myText.toString();
    if (! StringUtil.isEmptyOrSpaces(text)) {
      parseStdOut(text);
    }

    if (myGlobalError.length() > 0) {
      final String message = tryParseExceptionMessage(myGlobalError.toString());
      if (message != null) {
        myParsedGlobalError = parseGlobalError(message);
      } else {
        myParsedGlobalError = new JSLinterError(0,0,myGlobalError.toString(), null);
      }
    }
  }

  private static final String TYPE_ERROR = "TypeError:";
  private static final String ASSERTION_ERROR = "AssertionError:";
  private static final String OPTION_REQUIRES = "option requires";
  private static final String UNSUPPORTED_RULES = "Error: Unsupported rules:";
  private static JSLinterErrorBase parseGlobalError(@NotNull String message) {
    message = message.trim();
    String optionName;
    if (message.startsWith(ASSERTION_ERROR)) {
      final int endPhraseIdx = message.indexOf(OPTION_REQUIRES);
      if (endPhraseIdx > 0) {
        optionName = message.substring(ASSERTION_ERROR.length(), endPhraseIdx).trim();
        final JscsOption option = JscsOption.safeValueOf(optionName);
        if (option != null) {
          return new JSLinterErrorBase(message, option.name());
        }
      }
    } else if (message.startsWith(UNSUPPORTED_RULES)) {
      return new JSLinterErrorBase(message, message.substring(UNSUPPORTED_RULES.length()).trim());
    } else if (message.startsWith(TYPE_ERROR)) {
      return new JscsTypeError(message);
    }
    return new JSLinterErrorBase(message);
  }

  private void parseStdOut(String text) {
    final StdXMLBuilder builder = new StdXMLBuilder();
    NanoXmlUtil.parse(new StringReader(text), builder);
    final IXMLElement root = (IXMLElement) builder.getResult();
    if (root == null) {
      LOG.debug("JSCS: Failed to parse jscs output: " + text);
      return;
    }

    try {
      expect(root, "checkstyle");
      if (root.getChildrenCount() != 1) {
        LOG.debug("JSCS: not one file returned when asked for " + myTmpFile + " returned " + root.getChildrenCount());
        return;
      }
      final IXMLElement file = root.getChildAtIndex(0);
      expect(file, "file");

      final int numErrors = file.getChildrenCount();
      for (int i = 0; i < numErrors; i++) {
        final IXMLElement error = file.getChildAtIndex(i);
        expect(error, "error");
        parseError(error);
      }
    } catch (MyHomeException e) {
      LOG.debug("JSCS: expected " + e.getMessage() + " but found " + e.getElement().getName() + " Text:\n" + myText);
    }
  }

  public List<JSLinterError> getErrors() {
    return myErrors;
  }

  @Nullable
  public JscsObsoleteRulesWarning getObsoleteRulesWarning() {
    if (myObsoleteRulesUsed == null && myUnsupportedRulesUsed == null) return null;
    final List<String> rules = new ArrayList<>();
    if (myObsoleteRulesUsed != null) rules.addAll(myObsoleteRulesUsed);
    if (myUnsupportedRulesUsed != null) rules.addAll(myUnsupportedRulesUsed);
    return new JscsObsoleteRulesWarning("Unsupported rules: " + StringUtil.join(rules, ", "), rules);
  }

  public JSLinterErrorBase getGlobalError() {
    return myParsedGlobalError;
  }

  private void parseError(IXMLElement error) {
    String message = error.getAttribute("message", "");
    if (StringUtil.isEmptyOrSpaces(message)) return;
    final int colonIdx = message.indexOf(":");
    String code = colonIdx <= 0 ? "" : message.substring(0, colonIdx).trim();
    if (message.contains(RULE_IS_NO_LONGER_SUPPORTED) && message.contains(PLEASE_USE_OTHER)) {
      if (! code.isEmpty()) {
        if (myObsoleteRulesUsed == null) myObsoleteRulesUsed = new ArrayList<>();
        myObsoleteRulesUsed.add(code);
      }
      return;
    }
    if (message.contains(UNSUPPORTED_RULE)) {
      code = colonIdx <= 0 ? "" : message.substring(colonIdx + 1).trim();
      if (! code.isEmpty()) {
        if (myUnsupportedRulesUsed == null) myUnsupportedRulesUsed = new ArrayList<>();
        myUnsupportedRulesUsed.add(code);
      }
      return;
    }
    message = colonIdx > 0 ? message.substring(colonIdx + 1) : message;
    final Integer line = readLineOrCol(error, "line");
    final Integer col = readLineOrCol(error, "column");
    if (line != null && col != null) {
      myErrors.add(new JSLinterError(line, col, message.trim(), code));
    }
  }

  private static String tryParseExceptionMessage(@NotNull String message) {
    final String[] lines = message.split("\n");
    if (lines.length == 1) return null;
    boolean atFound = false;
    for (int i = lines.length - 1; i >= 0; i--) {
      final String line = lines[i].trim();
      if (line.isEmpty()) continue;
      if (isExceptionLine(line)) {
        atFound = true;
      } else {
        if (atFound) {
          LOG.debug("JSCS: " + message);
          return line;
        }
        return null;
      }
    }
    return null;
  }

  /*at files (D:\testProjects\ws\examples-koajs-master\node_modules\mocha\bin\_mocha:292:3)
at Array.forEach (native)
at node.js:902:3
*/
  private static boolean isExceptionLine(@NotNull final String s) {
    final int at = s.indexOf("at ");
    if (at < 0) return false;
    if (s.indexOf("native", at) > 0) return true;

    int from = at;
    while (from < s.length()) {
      int dotIdx = s.indexOf(":", from);
      if (dotIdx < 0 || dotIdx == (s.length() - 1)) return false;
      if (Character.isDigit(s.charAt(dotIdx + 1))) return true;
      from = dotIdx + 1;
    }
    return false;
  }

  private Integer readLineOrCol(@NotNull IXMLElement element, @NotNull final String name) {
    final String line = element.getAttribute(name, "");
    if (StringUtil.isEmptyOrSpaces(line)) {
      LOG.debug("JSCS: when checking file " + myTmpFile + " message without " + name + ": " + element.getAttribute("message", ""));
      return null;
    }
    try {
      return Integer.parseInt(line);
    } catch (NumberFormatException e) {
      LOG.debug("JSCS: when checking file " + myTmpFile + " " + name + " is not a number for message " + element.getAttribute("message", ""));
      return null;
    }
  }

  private static void expect(@NotNull final IXMLElement element, @NotNull final String name) throws MyHomeException {
    if (! name.equals(element.getName())) throw new MyHomeException(name, element);
  }

  private static class MyHomeException extends Exception {
    private final IXMLElement myElement;

    MyHomeException(String message, IXMLElement element) {
      super(message);
      myElement = element;
    }

    public IXMLElement getElement() {
      return myElement;
    }
  }
}
