package com.intellij.lang.javascript.linter.jscs.importer;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.intellij.lang.javascript.formatter.JSCodeStyleSettings;
import com.intellij.lang.javascript.linter.jscs.config.JscsOption;
import com.intellij.psi.codeStyle.CommonCodeStyleSettings;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * @author Irina.Chernushina on 4/20/2015.
 */
public class JscsRulesForCodeStyle {

  public static void fillRules(final Map<JscsOption, ImportRule> map,
                               final Map<JscsOption, PairImportRule> pairMap) {
    map.put(JscsOption.requireCurlyBraces, new AboutCurlyBraces(CommonCodeStyleSettings.FORCE_BRACES_ALWAYS));
    map.put(JscsOption.disallowCurlyBraces, new AboutCurlyBraces(CommonCodeStyleSettings.DO_NOT_FORCE));

    map.put(JscsOption.maximumLineLength, new MaxLineLen());

    map.put(JscsOption.requireSpaceAfterKeywords, new AboutSpaceAfterKeywords(true));
    map.put(JscsOption.disallowSpaceAfterKeywords, new AboutSpaceAfterKeywords(false));

    map.put(JscsOption.requireSpaceBeforeBlockStatements, new AboutBraceBeforeBlock(true));
    map.put(JscsOption.disallowSpaceBeforeBlockStatements, new AboutBraceBeforeBlock(false));

    map.put(JscsOption.requireSpacesInConditionalExpression, new AboutSpacesInConditionalExpression(true));
    map.put(JscsOption.disallowSpacesInConditionalExpression, new AboutSpacesInConditionalExpression(false));

    map.put(JscsOption.requireSpacesInFunctionDeclaration, new AboutSpacesInFunctionDeclaration(true));
    map.put(JscsOption.disallowSpacesInFunctionDeclaration, new AboutSpacesInFunctionDeclaration(false));

    map.put(JscsOption.requireSpacesInFunctionExpression, new AboutSpacesInFunctionExpression(true));
    map.put(JscsOption.disallowSpacesInFunctionExpression, new AboutSpacesInFunctionExpression(false));

    map.put(JscsOption.requireSpacesInFunction, new CompositeImportRule(new AboutSpacesInFunctionDeclaration(true),
                                                                        new AboutSpacesInFunctionExpression(true)));
    map.put(JscsOption.disallowSpacesInFunction, new CompositeImportRule(new AboutSpacesInFunctionDeclaration(false),
                                                                        new AboutSpacesInFunctionExpression(false)));

    map.put(JscsOption.validateIndentation, new ValidateIndentation());

    map.put(JscsOption.requireSpacesInsideArrayBrackets, new AboutSpacesInsideArrayBrackets(true));
    map.put(JscsOption.disallowSpacesInsideArrayBrackets, new AboutSpacesInsideArrayBrackets(false));

    map.put(JscsOption.validateQuoteMarks, new QuoteMarksRule());

    pairMap.put(JscsOption.requireSpaceBeforeBinaryOperators, new AboutAroundBinaryOperators(true));
    pairMap.put(JscsOption.disallowSpaceBeforeBinaryOperators, new AboutAroundBinaryOperators(false));

    pairMap.put(JscsOption.requireSpacesInAnonymousFunctionExpression, new AboutSpacesInAnonymousAndNamedFunctionExpressions(true));
    pairMap.put(JscsOption.disallowSpacesInAnonymousFunctionExpression, new AboutSpacesInAnonymousAndNamedFunctionExpressions(false));
  }

  private static boolean isTrue(JsonElement element) {
    return element != null && element.isJsonPrimitive() && ((JsonPrimitive) element).isBoolean() && element.getAsBoolean();
  }

  private static String getStringValue(JsonElement element) {
    return element != null && element.isJsonPrimitive() && ((JsonPrimitive) element).isString() ? element.getAsString() : null;
  }

  private static Integer isInteger(JsonElement element) {
    return element != null && element.isJsonPrimitive() && ((JsonPrimitive) element).isNumber() ? element.getAsInt() : null;
  }

  private static class AboutCurlyBraces extends ImportRule {
    private final int myDesiredOption;

    private AboutCurlyBraces(int option) {
      myDesiredOption = option;
    }

    @Override
    protected boolean applyImpl(JsonElement element, CommonCodeStyleSettings settings, JSCodeStyleSettings jsCodeStyleSettings) {
      if (isTrue(element)) {
        doIf(settings);
        doDoWhile(settings);
        doWhile(settings);
        doFor(settings);
        return true;
      } else if (element.isJsonArray()) {
        final JsonArray value = element.getAsJsonArray();
        boolean applied = false;
        for (int i = 0; i < value.size(); i ++) {
          final JsonElement arrElement = value.get(i);
          if (arrElement.isJsonPrimitive()) {
            boolean locallyApplied = true;
            if ("if".equals(getStringValue(arrElement))) doIf(settings);
            else if ("do".equals(getStringValue(arrElement))) doDoWhile(settings);
            else if ("while".equals(getStringValue(arrElement))) doWhile(settings);
            else if ("for".equals(getStringValue(arrElement))) doFor(settings);
            else locallyApplied = false;

            applied |= locallyApplied;
          }
        }
        return applied;
      }
      return false;
    }

    private void doFor(CommonCodeStyleSettings settings) {
      settings.FOR_BRACE_FORCE = myDesiredOption;
      append("Wrapping and Braces -> 'for()' statement = " + optionText());
    }

    private void doWhile(CommonCodeStyleSettings settings) {
      settings.WHILE_BRACE_FORCE = myDesiredOption;
      append("Wrapping and Braces -> 'while()' statement = " + optionText());
    }

    private void doDoWhile(CommonCodeStyleSettings settings) {
      settings.DOWHILE_BRACE_FORCE = myDesiredOption;
      append("Wrapping and Braces -> 'do ... while()' statement = " + optionText());
    }

    private void doIf(CommonCodeStyleSettings settings) {
      settings.IF_BRACE_FORCE = myDesiredOption;
      append("Wrapping and Braces -> 'if()' statement = " + optionText());
    }

    @NotNull
    private String optionText() {
      return myDesiredOption == CommonCodeStyleSettings.FORCE_BRACES_ALWAYS ? "Always" : "Do not force";
    }
  }

  private static class MaxLineLen extends ImportRule {
    @Override
    protected boolean applyImpl(JsonElement element, CommonCodeStyleSettings settings, JSCodeStyleSettings jsCodeStyleSettings) {
      if (isValueContainer(element, settings)) return true;
      if (element.isJsonObject()) {
        final JsonElement valueElement = element.getAsJsonObject().get("value");
        if (valueElement != null) {
          if (isValueContainer(valueElement, settings)) return true;
        }
      }
      return false;
    }

    private boolean isValueContainer(JsonElement element, CommonCodeStyleSettings settings) {
      final Integer value = isInteger(element);
      if (value != null) {
        settings.RIGHT_MARGIN = value;
        rightMarginSet(value);
        return true;
      }
      return false;
    }

    private void rightMarginSet(Integer value) {
      append("Wrapping and Braces -> Right margin (columns): = " + value);
    }
  }

  private static class AboutSpaceAfterKeywords extends ImportRule {
    private final boolean myValue;

    AboutSpaceAfterKeywords(boolean value) {
      myValue = value;
    }

    @Override
    protected boolean applyImpl(JsonElement element, CommonCodeStyleSettings settings, JSCodeStyleSettings jsCodeStyleSettings) {
      if (isTrue(element)) {
        doIf(settings);
        doFor(settings);
        doWhile(settings);
        doSwitch(settings);
        doCatch(settings);
        doFunctionExpr(settings);
        return true;
      } else if (element.isJsonArray()) {
        final JsonArray value = element.getAsJsonArray();
        boolean applied = false;
        for (int i = 0; i < value.size(); i ++) {
          final JsonElement arrElement = value.get(i);
          if (arrElement.isJsonPrimitive()) {
            boolean locallyApplied = true;
            if ("if".equals(getStringValue(arrElement))) doIf(settings);
            else if ("for".equals(getStringValue(arrElement))) doFor(settings);
            else if ("while".equals(getStringValue(arrElement))) doWhile(settings);
            else if ("switch".equals(getStringValue(arrElement))) doSwitch(settings);
            else if ("catch".equals(getStringValue(arrElement))) doCatch(settings);
            else if ("function".equals(getStringValue(arrElement))) doFunctionExpr(settings);
            else locallyApplied = false;

            applied |= locallyApplied;
          }
        }
        return applied;
      }
      return false;
    }

    private void doIf(CommonCodeStyleSettings settings) {
      settings.SPACE_BEFORE_IF_PARENTHESES = myValue;
      append("Spaces -> Before Parentheses -> 'if' parentheses = " + myValue);
    }

    private void doFor(CommonCodeStyleSettings settings) {
      settings.SPACE_BEFORE_FOR_PARENTHESES = myValue;
      append("Spaces -> Before Parentheses -> 'for' parentheses = " + myValue);
    }

    private void doWhile(CommonCodeStyleSettings settings) {
      settings.SPACE_BEFORE_WHILE_PARENTHESES = myValue;
      append("Spaces -> Before Parentheses -> 'while' parentheses = " + myValue);
    }

    private void doSwitch(CommonCodeStyleSettings settings) {
      settings.SPACE_BEFORE_SWITCH_PARENTHESES = myValue;
      append("Spaces -> Before Parentheses -> 'switch' parentheses = " + myValue);
    }

    private void doCatch(CommonCodeStyleSettings settings) {
      settings.SPACE_BEFORE_CATCH_PARENTHESES = myValue;
      append("Spaces -> Before Parentheses -> 'catch' parentheses = " + myValue);
    }

    private void doFunctionExpr(CommonCodeStyleSettings settings) {
      settings.SPACE_BEFORE_METHOD_PARENTHESES = myValue;
      append("Spaces -> Before Parentheses -> In function expression = " + myValue);
    }
  }

  private static class AboutBraceBeforeBlock extends ImportRule {
    private final boolean myValue;

    AboutBraceBeforeBlock(boolean value) {
      myValue = value;
    }

    @Override
    protected boolean applyImpl(JsonElement element, CommonCodeStyleSettings settings, JSCodeStyleSettings jsCodeStyleSettings) {
      if (isTrue(element)) {
        settings.SPACE_BEFORE_METHOD_LBRACE = myValue;
        settings.SPACE_BEFORE_IF_LBRACE = myValue;
        settings.SPACE_BEFORE_ELSE_LBRACE = myValue;
        settings.SPACE_BEFORE_FOR_LBRACE = myValue;
        settings.SPACE_BEFORE_WHILE_LBRACE = myValue;
        settings.SPACE_BEFORE_DO_LBRACE = myValue;
        settings.SPACE_BEFORE_SWITCH_LBRACE = myValue;
        settings.SPACE_BEFORE_TRY_LBRACE = myValue;
        settings.SPACE_BEFORE_CATCH_LBRACE = myValue;
        settings.SPACE_BEFORE_FINALLY_LBRACE = myValue;

        append("Spaces -> Before Left Brace -> (all values) = " + myValue);
        return true;
      }
      return false;
    }
  }

  private static class AboutSpacesInConditionalExpression extends ImportRule {
    private final boolean myValue;

    AboutSpacesInConditionalExpression(boolean value) {
      myValue = value;
    }

    @Override
    protected boolean applyImpl(JsonElement element, final CommonCodeStyleSettings settings, JSCodeStyleSettings jsCodeStyleSettings) {
      if (isTrue(element)) {
        doBeforeQ(settings);
        doAfterQ(settings);
        doBeforeColon(settings);
        doAfterColon(settings);
        return true;
      } else if (element.isJsonObject()) {
        final JsonObject object = element.getAsJsonObject();
        boolean applied = processOption(object, "afterTest", () -> doBeforeQ(settings));
        applied |= processOption(object, "beforeConsequent", () -> doAfterQ(settings));
        applied |= processOption(object, "afterConsequent", () -> doBeforeColon(settings));
        applied |= processOption(object, "beforeAlternate", () -> doAfterColon(settings));
        return applied;
      }
      return false;
    }

    private void doAfterColon(CommonCodeStyleSettings settings) {
      settings.SPACE_AFTER_COLON = myValue;
      append("Spaces -> In Ternary Operator (?:) -> After ':' = " + myValue);
    }

    private void doBeforeColon(CommonCodeStyleSettings settings) {
      settings.SPACE_BEFORE_COLON = myValue;
      append("Spaces -> In Ternary Operator (?:) -> Before ':' = " + myValue);
    }

    private void doAfterQ(CommonCodeStyleSettings settings) {
      settings.SPACE_AFTER_QUEST = myValue;
      append("Spaces -> In Ternary Operator (?:) -> After '?' = " + myValue);
    }

    private void doBeforeQ(CommonCodeStyleSettings settings) {
      settings.SPACE_BEFORE_QUEST = myValue;
      append("Spaces -> In Ternary Operator (?:) -> Before '?' = " + myValue);
    }
  }

  private static class AboutSpacesInFunctionDeclaration extends ImportRule {
    private final boolean myValue;

    private AboutSpacesInFunctionDeclaration(boolean value) {
      myValue = value;
    }

    @Override
    protected boolean applyImpl(JsonElement element, final CommonCodeStyleSettings settings, JSCodeStyleSettings jsCodeStyleSettings) {
      if (element.isJsonObject()) {
        final JsonObject object = element.getAsJsonObject();
        boolean applied = processOption(object, "beforeOpeningRoundBrace", () -> {
          settings.SPACE_BEFORE_METHOD_PARENTHESES = myValue;
          append("Spaces -> Before Parentheses -> Function declaration parentheses = " + myValue);
        });
        applied |= processOption(object, "beforeOpeningCurlyBrace", () -> {
          // this property is both for declaration and expression
          settings.SPACE_BEFORE_METHOD_LBRACE = myValue;
          append("Spaces -> Before Left Brace -> Function left brace = " + myValue);
        });
        return applied;
      }
      return false;
    }
  }

  private static class AboutSpacesInFunctionExpression extends ImportRule {
    private final boolean myValue;

    AboutSpacesInFunctionExpression(boolean value) {
      myValue = value;
    }

    @Override
    protected boolean applyImpl(JsonElement element, final CommonCodeStyleSettings settings, final JSCodeStyleSettings jsCodeStyleSettings) {
      if (element.isJsonObject()) {
        final JsonObject object = element.getAsJsonObject();
        boolean applied = processOption(object, "beforeOpeningRoundBrace", () -> {
          jsCodeStyleSettings.SPACE_BEFORE_FUNCTION_LEFT_PARENTH = myValue;
          append("Spaces -> Before Parentheses -> In function expression = " + myValue);
        });
        applied |= processOption(object, "beforeOpeningCurlyBrace", () -> {
          settings.SPACE_BEFORE_METHOD_LBRACE = myValue;
          append("Spaces -> Before Left Brace -> Function left brace = " + myValue);
        });
        return applied;
      }
      return false;
    }
  }

  private static boolean processOption(final JsonObject object, final String name, final Runnable action) {
    if (isOptionSet(object, name)) {
      action.run();
      return true;
    }
    return false;
  }

  private static boolean isOptionSet(final @NotNull JsonObject object, final @NotNull String name) {
    final JsonElement element = object.get(name);
    return element != null && isTrue(element);
  }

  private static class ValidateIndentation extends ImportRule {
    @Override
    protected boolean applyImpl(JsonElement element, CommonCodeStyleSettings settings, JSCodeStyleSettings jsCodeStyleSettings) {
      if (doValue(element, settings)) {
        return true;
      } else if (isTab(element)) {
        settings.getIndentOptions().USE_TAB_CHARACTER = true;
        append("Tabs and Indents -> Use tab character = true");
        return true;
      } else if (element.isJsonObject()) {
        final JsonObject object = element.getAsJsonObject();
        boolean applied = false;
        JsonElement value = object.get("value");
        if (value != null) {
          applied = doValue(value, settings);
        }
        final JsonElement emptyLines = object.get("includeEmptyLines");
        if (isTrue(emptyLines)) {
          settings.getIndentOptions().KEEP_INDENTS_ON_EMPTY_LINES = true;
          append("Tabs and Indents -> Keep indents on empty lines = true");
          applied = true;
        }
        return applied;
      }
      return false;
    }

    private boolean isTab(JsonElement element) {
      return element.isJsonPrimitive() && element.getAsJsonPrimitive().isString() && "\t".equals(element.getAsString());
    }

    private boolean doValue(JsonElement element, CommonCodeStyleSettings settings) {
      Integer integer = isInteger(element);
      if (integer != null) {
        settings.getIndentOptions().INDENT_SIZE = integer;
        append("Tabs and Indents -> Indent = " + integer);
        return true;
      }
      if (isTab(element)) {
        settings.getIndentOptions().USE_TAB_CHARACTER = true;
        append("Tabs and Indents -> Use tab character = true");
        return true;
      }
      return false;
    }
  }

  private static class AboutSpacesInsideArrayBrackets extends ImportRule {
    private final boolean myValue;

    AboutSpacesInsideArrayBrackets(boolean value) {
      myValue = value;
    }

    @Override
    protected boolean applyImpl(JsonElement element, CommonCodeStyleSettings settings, JSCodeStyleSettings jsCodeStyleSettings) {
      if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isString() && "all".equals(element.getAsString())) {
        jsCodeStyleSettings.SPACE_WITHIN_ARRAY_INITIALIZER_BRACKETS = myValue;
      }
      return false;
    }
  }

  private static class AboutAroundBinaryOperators extends PairImportRule {
    private final boolean myValue;

    AboutAroundBinaryOperators(boolean value) {
      myValue = value;
    }

    @Override
    public JscsOption getPairRule() {
      return myValue ? JscsOption.requireSpaceAfterBinaryOperators : JscsOption.disallowSpaceAfterBinaryOperators;
    }

    @Override
    protected boolean applyImpl(JsonElement element1,
                                JsonElement element2,
                                CommonCodeStyleSettings settings,
                                JSCodeStyleSettings jsCodeStyleSettings) {
      if (isTrue(element1) && isTrue(element2)) {
        settings.SPACE_AROUND_ASSIGNMENT_OPERATORS = myValue;
        settings.SPACE_AROUND_LOGICAL_OPERATORS = myValue;
        settings.SPACE_AROUND_EQUALITY_OPERATORS = myValue;
        settings.SPACE_AROUND_RELATIONAL_OPERATORS = myValue;

        settings.SPACE_AROUND_BITWISE_OPERATORS = myValue;
        settings.SPACE_AROUND_ADDITIVE_OPERATORS = myValue;
        settings.SPACE_AROUND_MULTIPLICATIVE_OPERATORS = myValue;
        settings.SPACE_AROUND_SHIFT_OPERATORS = myValue;

        append("Spaces -> Around Operators -> (all binary) = " + myValue);
        return true;
      }
      return false;
    }
  }

  private static class AboutSpacesInAnonymousAndNamedFunctionExpressions extends PairImportRule {
    @NonNls
    public static final String BEFORE_OPENING_ROUND_BRACE = "beforeOpeningRoundBrace";
    @NonNls
    public static final String BEFORE_OPENING_CURLY_BRACE = "beforeOpeningCurlyBrace";
    private final boolean myValue;

    AboutSpacesInAnonymousAndNamedFunctionExpressions(final boolean value) {
      myValue = value;
    }

    @Override
    public JscsOption getPairRule() {
      return myValue ? JscsOption.requireSpacesInNamedFunctionExpression : JscsOption.disallowSpacesInNamedFunctionExpression;
    }

    @Override
    protected boolean applyImpl(JsonElement element1,
                                JsonElement element2,
                                CommonCodeStyleSettings settings,
                                JSCodeStyleSettings jsCodeStyleSettings) {
      if (element1.isJsonObject() && element2.isJsonObject()) {
        final JsonObject object1 = element1.getAsJsonObject();
        final JsonObject object2 = element2.getAsJsonObject();
        if (isOptionSet(object1, BEFORE_OPENING_ROUND_BRACE) && isOptionSet(object2, BEFORE_OPENING_ROUND_BRACE)) {
          jsCodeStyleSettings.SPACE_BEFORE_FUNCTION_LEFT_PARENTH = myValue;
          append("Spaces -> Before Parentheses -> In function expression = " + myValue);
        }
        if (isOptionSet(object1, BEFORE_OPENING_CURLY_BRACE) && isOptionSet(object2, BEFORE_OPENING_CURLY_BRACE)) {
          settings.SPACE_BEFORE_METHOD_LBRACE = myValue;
          append("Spaces -> Before Left Brace -> Function left brace = " + myValue);
        }
      }

      return false;
    }
  }

  private static class QuoteMarksRule extends ImportRule {
    @Override
    protected boolean applyImpl(JsonElement element, CommonCodeStyleSettings settings, JSCodeStyleSettings jsCodeStyleSettings) {
      String value = getStringValue(element);
      if (value == null && element.isJsonObject()) {
        final JsonElement mark = element.getAsJsonObject().get("mark");
        if (mark != null) {
          value = getStringValue(mark);
        }
      }
      Boolean isDouble = null;
      if ("\"".equals(value)) isDouble = true;
      if ("'".equals(value)) isDouble = false;
      if (isDouble != null) {
        jsCodeStyleSettings.USE_DOUBLE_QUOTES = isDouble;
        return true;
      }
      return false;
    }
  }
}
