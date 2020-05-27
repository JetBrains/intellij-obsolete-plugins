package com.intellij.lang.javascript.linter.jscs.config;

import com.intellij.json.psi.JsonArray;
import com.intellij.json.psi.JsonBooleanLiteral;
import com.intellij.json.psi.JsonStringLiteral;
import com.intellij.json.psi.JsonValue;
import com.intellij.lang.javascript.linter.jscs.JscsPreset;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.ObjectUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.intellij.lang.javascript.linter.jscs.config.JscsOptionDescriptor.ValueDescription.NO_VALIDATION;
import static com.intellij.lang.javascript.linter.jscs.config.ValueType.*;

/**
 * @author Irina.Chernushina on 4/30/2015.
 */
public enum JscsOption {
  requireCurlyBraces                          (boolOrKeywordsArray()),
  requireSpaceAfterKeywords                   (boolOrKeywordsArray()),
  disallowSpaceAfterKeywords                  (boolOrKeywordsArray()),
  requireSpaceBeforeBlockStatements           (descr().canBool().canInteger()),
  disallowSpaceBeforeBlockStatements          (descr().canBool()),
  requireParenthesesAroundIIFE                (descr().canBool()),
  requireSpacesInConditionalExpression        (ternary()),
  disallowSpacesInConditionalExpression       (ternary()),
  requireSpacesInFunctionExpression           (beforeBraces()),
  disallowSpacesInFunctionExpression          (beforeBraces()),
  requireSpacesInAnonymousFunctionExpression  (descr().addType(obj, new JscsOptionDescriptor.ValuesObject((o) -> {
      o.addBoolFields("beforeOpeningRoundBrace", "beforeOpeningCurlyBrace");
      o.addField("allExcept").addType(array, new JscsOptionDescriptor.CustomValidation((value) -> {
          final JsonArray array = ObjectUtils.tryCast(value, JsonArray.class);
          final String expected = "Expected values: true or \"shorthand\"";

          if (array != null) {
            // null is not gonna happen, it is checked in annotator
            final List<JsonValue> list = array.getValueList();
            if (list.size() > 1) return "Expected one-element array";
            if (list.isEmpty()) return null;

            final JsonValue arrayValue = list.get(0);
            if (arrayValue instanceof JsonBooleanLiteral) {
              if ("true".equals(arrayValue.getText())) return null;
            } else if (arrayValue instanceof JsonStringLiteral) {
              if ("shorthand".equals(StringUtil.unquoteString(arrayValue.getText()))) return null;
            }
            return expected;
          }
          return null;
      }));
    }))),
  disallowSpacesInAnonymousFunctionExpression (beforeBraces()),
  requireSpacesInNamedFunctionExpression      (beforeBraces()),
  disallowSpacesInNamedFunctionExpression     (beforeBraces()),
  requireSpacesInFunctionDeclaration          (beforeBraces()),
  disallowSpacesInFunctionDeclaration         (beforeBraces()),
  requireSpacesInFunction                     (beforeBraces()),
  disallowSpacesInFunction                    (beforeBraces()),
  disallowMultipleVarDecl                     (descr().canBool().addType(obj, new JscsOptionDescriptor.ValuesObject((o)-> {
      o.addField("strict").canBool();
      o.addField("allExcept").canStringArray("undefined", "required");
  }))),
  requireMultipleVarDecl                      (descr().canBool().addType(str, new JscsOptionDescriptor.ValuesCollection("onevar"))),
  requireBlocksOnNewline                      (descr().canBool().canInteger().addType(obj, new JscsOptionDescriptor.ValuesObject((o)-> {
      o.addField("includeComments").canBool();
      o.addField("minLines").canInteger();
  }))),
  requirePaddingNewlinesInBlocks              (descr().canBool().canInteger().addType(obj, new JscsOptionDescriptor.ValuesObject((o)-> {
      o.addField("open").canTrueOrFalse();
      o.addField("close").canTrueOrFalse();
  }))),
  disallowPaddingNewlinesInBlocks             (descr().canBool()),
  disallowSpacesInsideObjectBrackets          (descr().canBool().addType(str, new JscsOptionDescriptor.ValuesCollection("all", "nested"))
    .addType(obj, new JscsOptionDescriptor.ValuesObject((o)-> o.addField("allExcept").canStringArray("}", ")", "{", "(")))),
  disallowSpacesInsideArrayBrackets           (descr().canBool().addType(str, new JscsOptionDescriptor.ValuesCollection("all", "nested"))
                                                 .addType(obj, new JscsOptionDescriptor.ValuesObject((o)-> o.addField("allExcept").canStringArray("}", "]", "{", "[")))),
  disallowSpacesInsideParentheses             (descr().canBool().addType(obj, new JscsOptionDescriptor.ValuesObject((o)-> o.addField("only").canStringArray("{", "}", "(", ")")))),
  requireSpacesInsideObjectBrackets           (descr().addType(str, new JscsOptionDescriptor.ValuesCollection("all", "allButNested"))
                                                 .addType(obj, new JscsOptionDescriptor.ValuesObject((o)-> o.addField("allExcept").canStringArray("}", ")", "{", "(")))),
  requireSpacesInsideArrayBrackets            (descr().addType(str, new JscsOptionDescriptor.ValuesCollection("all", "allButNested"))
                                                 .addType(obj, new JscsOptionDescriptor.ValuesObject((o)-> o.addField("allExcept").canStringArray("}", "]", "{", "[")))),
  requireSpacesInsideParentheses              (descr().addType(str, new JscsOptionDescriptor.ValuesCollection("all", "allButNested"))
                                                 .addType(obj, new JscsOptionDescriptor.ValuesObject((o)-> {
                                                     o.addField("all").canBool();
                                                     o.addField("except").canStringArray("}", ")", "{", "(");
                                                 }))),
  disallowQuotedKeysInObjects                 (descr().canBool().canString("allButReserved")),
  disallowDanglingUnderscores                 (descr().canBool().addType(obj, new JscsOptionDescriptor.ValuesObject((o)-> o.addField("allExcept").addType(array, NO_VALIDATION)))),
  disallowSpaceAfterObjectKeys                (descr().canBool().canString("ignoreSingleLine", "ignoreMultiLine")
                                                .addType(obj, new JscsOptionDescriptor.ValuesObject((o)-> o.addField("allExcept").canStringArray("singleline", "multiline", "aligned")))),
  requireSpaceAfterObjectKeys                 (descr().canBool()),
  disallowSpaceBeforeObjectValues             (descr().canBool()),
  requireSpaceBeforeObjectValues              (descr().canBool()),
  disallowCommaBeforeLineBreak                (descr().canBool(). addType(obj, new JscsOptionDescriptor.ValuesObject((o)-> o.addField("allExcept").canStringArray("function")))),
  requireCommaBeforeLineBreak                 (descr().canBool()),
  requireAlignedObjectValues                  (descr().canString("all", "ignoreFunction", "ignoreLineBreak")),

  requireOperatorBeforeLineBreak              (descr().canBool().canOperatorsArray()),
  disallowSpaceAfterPrefixUnaryOperators      (descr().canBool().canStringArray(Constants.unaryOperators)),
  requireSpaceAfterPrefixUnaryOperators       (descr().canBool().canStringArray(Constants.unaryOperators)),
  disallowSpaceBeforePostfixUnaryOperators    (descr().canBool().canStringArray(Constants.unaryOperators)),
  requireSpaceBeforePostfixUnaryOperators     (descr().canBool().canStringArray(Constants.unaryOperators)),

  disallowSpaceBeforeBinaryOperators          (descr().canBool().canBinaryOperatorsArray()),
  requireSpaceBeforeBinaryOperators           (descr().canBool().canBinaryOperatorsArray()),
  disallowSpaceAfterBinaryOperators           (descr().canBool().canBinaryOperatorsArray()),
  requireSpaceAfterBinaryOperators            (descr().canBool().canBinaryOperatorsArray()),

  disallowSpaceBeforeComma                    (descr().canBool()),
  requireSpaceBeforeComma                     (descr().canBool()),
  disallowSpaceBeforeSemicolon                (descr().canBool()),
  disallowParenthesesAroundArrowParam         (descr().canBool()),
  requireParenthesesAroundArrowParam          (descr().canBool()),
  disallowObjectKeysOnNewLine                 (descr().canBool()),
  requireObjectKeysOnNewLine                  (descr().canBool().addType(obj, new JscsOptionDescriptor.ValuesObject((o)-> o.addField("allExcept").canStringArray("sameLine")))),

  disallowImplicitTypeConversion              (descr().canStringArray("numeric", "boolean", "binary", "string")),
  requireCamelCaseOrUpperCaseIdentifiers      (descr().canBool().canString("ignoreProperties")),
  disallowKeywords                            (descr().canKeywordsArray()),
  disallowMultipleLineStrings                 (descr().canBool()),
  disallowMultipleLineBreaks                  (descr().canBool()),
  disallowMixedSpacesAndTabs                  (descr().canBool().canString("smart")),
  disallowTrailingWhitespace                  (descr().canBool().canString("ignoreEmptyLines")),
  disallowTrailingComma                       (descr().canBool()),
  requireTrailingComma                        (descr().canBool().addType(obj, new JscsOptionDescriptor.ValuesObject(o-> o.addBoolFields("ignoreSingleValue", "ignoreSingleLine")))),
  disallowKeywordsOnNewLine                   (descr().canKeywordsArray()),
  requireKeywordsOnNewLine                    (descr().canKeywordsArray()),
  requireLineFeedAtFileEnd                    (descr().canBool()),
  maximumLineLength                           (descr().canInteger().addType(obj, new JscsOptionDescriptor.ValuesObject((o)-> {
      o.addField("value").canInteger();
      o.addField("tabSize").canInteger();
      o.addField("allExcept").canStringArray("regex", "comments", "urlComments", "functionSignature", "require");
      o.addBoolFields("allowComments", "allowUrlComments", "allowRegex");
  }).mandatory("value"))),
  requireCapitalizedConstructors              (descr().canBool().addType(obj, new JscsOptionDescriptor.ValuesObject((o)-> o.addField("allExcept").addType(array, NO_VALIDATION)))),
  requireDotNotation                          (descr().canBool().canString("except_snake_case")),
  requireYodaConditions                       (descr().canBool().canStringArray(Constants.conditionalOperators)),
  disallowYodaConditions                      (descr().canBool().canStringArray(Constants.conditionalOperators)),
  requireSpaceAfterLineComment                (descr().canBool().canString("allowSlash").addType(obj, new JscsOptionDescriptor.ValuesObject((o)-> o.addField("allExcept").addType(array, NO_VALIDATION)))),
  disallowSpaceAfterLineComment               (descr().canBool()),
  disallowAnonymousFunctions                  (descr().canBool()),
  requireAnonymousFunctions                   (descr().canBool().addType(obj, new JscsOptionDescriptor.ValuesObject((o)-> o.addField("allExcept").canStringArray("declarations")))),
  disallowNewlineBeforeBlockStatements        (descr().canBool().canStringArray(Constants.blockStatementKeywords)),
  requireNewlineBeforeBlockStatements         (descr().canBool().canStringArray(Constants.blockStatementKeywords)),
  validateLineBreaks                          (descr().canString("CR", "LF", "CRLF")),
  validateQuoteMarks                          (descr().canBool().canString("\"", "'")
    .addType(obj, new JscsOptionDescriptor.ValuesObject((o)-> {
        o.addBoolFields("escape");
        o.addField("mark").canString("\"", "'");
    }))),
  validateIndentation                         (descr().canInteger().addType(str, new JscsOptionDescriptor.ValuesCollection("\t").doNotTrimValues())
                                                 .addType(obj, new JscsOptionDescriptor.ValuesObject((o)-> {
      o.addField("value").canInteger().addType(str, new JscsOptionDescriptor.ValuesCollection("\t").doNotTrimValues());
      o.addBoolFields("includeEmptyLines");
      o.addField("allExcept").canStringArray("comments", "emptyLines");
  }).mandatory("value"))),
  validateParameterSeparator                  (descr().addType(str, new JscsOptionDescriptor.ValuesCollection(", ", " ,", " , ").doNotTrimValues())),
  jsDoc                                       (descr().addType(obj, new JscsOptionDescriptor.ValuesObject((o)-> {
      o.addField("checkAnnotations").canBool().canString("closurecompiler", "jsdoc3", "jsduck5")
        .addType(obj, new JscsOptionDescriptor.ValuesObject((i)-> {
            i.addField("preset").canString("closurecompiler", "jsdoc3", "jsduck5");
            i.addField("extra").addType(obj, NO_VALIDATION);
        }))
        .withDescription("Ensures tag names are valid");
      o.addField("checkParamExistence").canBool()
        .withDescription("Checks all parameters are documented");
      o.addField("checkParamNames").canBool()
        .withDescription("Ensures param names in jsdoc and in function declaration are equal");
      o.addField("requireParamTypes").canBool()
        .withDescription("Ensures params in jsdoc contains type");
      o.addField("checkRedundantParams").canBool()
        .withDescription("Reports redundant params in jsdoc");
      o.addField("checkReturnTypes").canBool()
        .withDescription("Reports discrepancies between the claimed in jsdoc and actual typ");
      o.addField("checkRedundantReturns").canBool()
        .withDescription("Report statements for functions with no return");
      o.addField("requireReturnTypes").canBool()
        .withDescription("Ensures returns in jsdoc contains type");
      o.addField("checkTypes").canBool().canString("strictNativeCase", "capitalizedNativeCase")
        .withDescription("Reports invalid types for bunch of tags");
      o.addField("checkRedundantAccess").canBool().canString("enforceLeadingUnderscore", "enforceTrailingUnderscore")
        .withDescription("Reports redundant access declarations");
      o.addField("leadingUnderscoreAccess").canBool().canString("private", "protected")
        .withDescription("Ensures access declaration is set for _underscored function names");
      o.addField("enforceExistence").canBool().canString("exceptExports")
        .withDescription("Ensures jsdoc block exist");
      o.addField("requireHyphenBeforeDescription").canBool()
        .withDescription("Ensures a param description has a hyphen before it (checks for -)");
      o.addField("requireNewlineAfterDescription").canBool()
        .withDescription("Ensures a doc comment description has padding newline");
      o.addField("disallowNewlineAfterDescription").canBool()
        .withDescription("Ensures a doc comment description has no padding newlines");
      o.addField("requireDescriptionCompleteSentence").canBool()
        .withDescription("Ensures a doc comment description is a complete sentence.");
      o.addField("requireParamDescription").canBool()
        .withDescription("Ensures a param description exists.");
      o.addField("requireReturnDescription").canBool()
        .withDescription("Checks a return description exists");
  }))),
  safeContextKeyword                          (descr().addType(str, NO_VALIDATION)
    .addType(array, NO_VALIDATION)),
  disallowPaddingNewlinesBeforeKeywords       (descr().canBool().canKeywordsArray()),
  disallowMultipleSpaces                      (descr().canBool()),
  disallowKeywordsInComments                  (descr().canBool().addType(str, new JscsOptionDescriptor.StringVerifier((text) -> {
    if (! text.startsWith("\\b(") || ! text.endsWith(")\\b")) {
      return "String should be defined in format '\\b(word1|word2)\\b'";
    }
    return null;
  }))
    .addType(array, NO_VALIDATION)),
  requireCapitalizedComments                  (descr().canBool().addType(obj, new JscsOptionDescriptor.ValuesObject((o)-> o.addField("allExcept").addType(array, NO_VALIDATION)))),
  disallowSpaceBeforeKeywords                 (descr().canBool().canKeywordsArray()),
  requirePaddingNewLinesBeforeLineComments    (descr().canBool().addType(obj, new JscsOptionDescriptor.ValuesObject((o)-> o.addField("allExcept").canString("firstAfterCurly")))),
  requireDollarBeforejQueryAssignment         (descr().canBool().canString("ignoreProperties")),
  disallowFunctionDeclarations                (descr().canBool()),
  requireSpaceBeforeKeywords                  (descr().canBool().canKeywordsArray()),
  disallowCapitalizedComments                 (descr().canBool()),
  disallowSpaceBetweenArguments               (descr().canBool()),
  disallowSpacesInForStatement                (descr().canBool()),
  requirePaddingNewLinesAfterBlocks           (descr().canBool().addType(obj, new JscsOptionDescriptor.ValuesObject((o)-> o.addField("allExcept").canStringArray("inCallExpressions", "inNewExpressions", "inArrayExpressions", "inProperties")))),
  requirePaddingNewlinesBeforeKeywords        (descr().canBool().canKeywordsArray()),
  disallowPaddingNewLinesBeforeLineComments   (descr().canBool()),
  requireLineBreakAfterVariableAssignment     (descr().canBool()),
  disallowCurlyBraces                         (descr().canBool().canKeywordsArray()),
  disallowOperatorBeforeLineBreak             (descr().canBool().canOperatorsArray()),
  disallowPaddingNewLinesAfterBlocks          (descr().canBool()),
  disallowSemicolons                          (descr().canBool()),
  requireSemicolons                           (descr().canBool()),
  requireSpaceBetweenArguments                (descr().canBool()),
  disallowSpacesInCallExpression              (descr().canBool()),
  disallowIdentifierNames                     (descr().addType(array, NO_VALIDATION)),
  requirePaddingNewLineAfterVariableDeclaration (descr().canBool()),
  requirePaddingNewLinesInObjects             (descr().canBool()),
  disallowPaddingNewLinesInObjects            (descr().canBool()),
  requireSpacesInForStatement                 (descr().canBool()),
  disallowSpacesInsideBrackets                (descr().canBool().addType(obj, new JscsOptionDescriptor.ValuesObject((o)-> o.addField("allExcept").canStringArray("[", "]", "{", "}")))),
  requireSpacesInCallExpression               (descr().canBool()),
  requireQuotedKeysInObjects                  (descr().canBool()),
  requireFunctionDeclarations                 (descr().canBool()),
  requireSpacesInsideBrackets                 (descr().canBool().addType(obj, new JscsOptionDescriptor.ValuesObject((o)-> o.addField("allExcept").canStringArray("[", "]", "{", "}")))),
  disallowNamedUnassignedFunctions            (descr().canBool()),
  disallowNotOperatorsInConditionals          (descr().canBool()),
  disallowPaddingNewLinesAfterUseStrict       (descr().canBool()),
  disallowPaddingNewLinesBeforeExport         (descr().canBool()),
  maximumNumberOfLines                        (descr().canInteger()),
  requireNamedUnassignedFunctions             (descr().canBool().addType(obj, new JscsOptionDescriptor.ValuesObject((o)-> o.addField("allExcept").addType(array, NO_VALIDATION)))),
  requirePaddingNewLinesBeforeExport          (descr().canBool()),
  requireVarDeclFirst                         (descr().canBool()),
  requireMatchingFunctionName                 (descr().canBool()),
  requireTemplateStrings                      (descr().canBool().addType(obj, new JscsOptionDescriptor.ValuesObject((o)-> o.addField("allExcept").canStringArray("stringConcatenation")))),
  requireSpread                               (descr().canBool()),
  requireShorthandArrowFunctions              (descr().canBool()),
  requireArrowFunctions                       (descr().canBool()),
  disallowNodeTypes                           (descr().canExpandableStringArray()),
  requireNumericLiterals                      (descr().canBool()),
  validateAlignedFunctionParameters           (descr().canBool().addType(obj, new JscsOptionDescriptor.ValuesObject((o)-> o.addBoolFields("lineBreakAfterOpeningBraces", "lineBreakBeforeClosingBraces")))),
  validateNewlineAfterArrayElements           (descr().canBool().canInteger().addType(obj, new JscsOptionDescriptor.ValuesObject((o)-> {
      o.addField("maximum").canInteger();
      o.addBoolFields("ignoreBrackets");
  }))),

  disallowShorthandArrowFunctions             (descr().canBool()),
  disallowArrowFunctions                      (descr().canBool()),
  validateOrderInObjectKeys                   (descr().canBool().canString("asc", "asc-insensitive", "asc-natural",
                                                                           "desc", "desc-insensitive", "desc-natural")),
  disallowEmptyBlocks                         (descr().canBool().addType(obj, new JscsOptionDescriptor.ValuesObject((o)-> o.addField("allExcept").canStringArray("comments")))),
  requirePaddingNewLinesAfterUseStrict        (descr().canBool().addType(obj, new JscsOptionDescriptor.ValuesObject((o)-> o.addField("allExcept").canStringArray("require")))),

  // 02/2016
  disallowIdenticalDestructuringNames         (descr().canBool()),
  disallowMultiLineTernary                    (descr().canBool()),
  disallowNestedTernaries                     (descr().canBool().addType(obj, new JscsOptionDescriptor.ValuesObject((o)-> o.addField("maxLevel").canInteger()))),
  disallowSpaceAfterComma                     (descr().canBool().addType(obj, new JscsOptionDescriptor.ValuesObject((o)-> o.addField("allExcept").canStringArray("sparseArrays")))),
  disallowSpacesInGenerator                   (spacesInGenerator()),
  disallowSpacesInsideParenthesizedExpression (spacesInParenthesizedExpression()),
  disallowSpacesInsideTemplateStringPlaceholders (descr().canBool()),
  disallowTabs                                (descr().canBool()),
  disallowUnusedParams                        (descr().canBool()),
  disallowVar                                 (descr().canBool()),
  requireAlignedMultilineParams               (descr().canBool().canInteger().canString("firstParam")),
  requireArrayDestructuring                   (descr().canBool()),
  requireCapitalizedConstructorsNew           (descr().canBool().addType(obj, new JscsOptionDescriptor.ValuesObject((o)-> o.addField("allExcept").canExpandableStringArray()))),
  requireEarlyReturn                          (descr().canBool()),
  requireEnhancedObjectLiterals               (descr().canBool()),
  requireImportAlphabetized                   (descr().canBool()),
  requireMultiLineTernary                     (descr().canBool()),
  requireObjectDestructuring                  (descr().canBool()),
  requireSpaceAfterComma                      (descr().canBool().addType(obj, new JscsOptionDescriptor.ValuesObject((o)-> o.addField("allExcept").canStringArray("trailing")))),
  requireSpacesInGenerator                    (spacesInGenerator()),
  requireSpacesInsideParenthesizedExpression  (spacesInParenthesizedExpression()),
  validateCommentPosition                     (descr().addType(obj, new JscsOptionDescriptor.ValuesObject((o)-> {
      o.addField("position").canString("above", "beside");
      o.addField("allExcept").canExpandableStringArray();
  }))),
  disallowArrayDestructuringReturn            (descr().canBool()),
  disallowSpacesInsideImportedObjectBraces    (descr().canBool()),
  disallowUnusedVariables                     (descr().canBool()),
  requireNewlineBeforeSingleStatementsInIf    (descr().canBool()),
  requireSpaceBeforeDestructuredValues        (descr().canBool()),
  requireSpacesInsideImportedObjectBraces     (descr().canBool()),
  requireUseStrict                            (descr().canBool()),

  // options, not rules below
  additionalRules                             (descr().addType(array, NO_VALIDATION)),
  preset                                      (descr().canString(JscsPreset.stringValues())),
  excludeFiles                                (descr().addType(array, NO_VALIDATION)),
  fileExtensions                              (descr().addType(str, new JscsOptionDescriptor.StringVerifier((text) -> {
    final String trim = text.trim();
    if ("*".equals(trim)) return null;
    return "Expected array of file extensions, beginning with '.', or \"*\"";
  })).
    addType(array, new JscsOptionDescriptor.StringVerifier((text -> {
      final String trim = text.trim();
      if (trim.startsWith(".")) return null;
      return "File extension should begin with '.'";

    })))),
  extract                                     (descr().canBool().canExpandableStringArray()),
  maxErrors                                   (descr().canInteger()),
  es3                                         (descr().canBool()),
  errorFilter                                 (descr().addType(str, NO_VALIDATION)),
  plugins                                     (descr().addType(array, NO_VALIDATION));

  private static JscsOptionDescriptor spacesInParenthesizedExpression() {
    return descr().canBool().addType(obj, new JscsOptionDescriptor.ValuesObject((o)-> o.addField("allExcept").canStringArray("{", "}", "function")));
  }

  private static JscsOptionDescriptor spacesInGenerator() {
    return descr().addType(obj, new JscsOptionDescriptor.ValuesObject((o)-> {
        o.addField("beforeStar").canBool();
        o.addField("afterStar").canBool();
        o.mustNotBeEmpty();
    }));
  }

  static {
    disallowAnonymousFunctions.setDescription("Requires that a function expression be named.");
    disallowCapitalizedComments.setDescription("Requires the first alphabetical character of a comment to be lowercase.");
    disallowCommaBeforeLineBreak.setDescription("Disallows commas as last token on a line in lists.");
    disallowCurlyBraces.setDescription("Disallows curly braces after statements.");
    disallowDanglingUnderscores.setDescription("Disallows identifiers that start or end in _. Some popular identifiers are automatically listed as exceptions:");
    disallowEmptyBlocks.setDescription("Disallows empty blocks (except for catch blocks).");
    disallowFunctionDeclarations.setDescription("Disallows function declarations.");
    disallowIdentifierNames.setDescription("Disallows a specified set of identifier names.");
    disallowImplicitTypeConversion.setDescription("Disallows implicit type conversion.");
    disallowKeywordsInComments.setDescription("Disallows keywords in your comments, such as TODO or FIXME");
    disallowKeywordsOnNewLine.setDescription("Disallows placing keywords on a new line.");
    disallowKeywords.setDescription("Disallows usage of specified keywords.");
    disallowMixedSpacesAndTabs.setDescription("Requires lines to not contain both spaces and tabs consecutively, or spaces after tabs only for alignment if \"smart\"");
    disallowMultipleLineBreaks.setDescription("Disallows multiple blank lines in a row.");
    disallowMultipleLineStrings.setDescription("Disallows strings that span multiple lines without using concatenation.");
    disallowMultipleSpaces.setDescription("Disallows multiple indentation characters (tabs or spaces) between identifiers, keywords, and any other token");
    disallowMultipleVarDecl.setDescription("Disallows multiple var declaration (except for-loop).");
    disallowNamedUnassignedFunctions.setDescription("Disallows unassigned functions to be named inline");
    disallowNewlineBeforeBlockStatements.setDescription("Disallows newline before opening curly brace of all block statements.");
    disallowNotOperatorsInConditionals.setDescription("Disallows the not, not equals, and strict not equals operators in conditionals.");
    disallowOperatorBeforeLineBreak.setDescription("Requires putting certain operators on the next line rather than on the current line before a line break.");
    disallowPaddingNewLinesAfterBlocks.setDescription("Disallow a newline after blocks");
    disallowPaddingNewLinesAfterUseStrict.setDescription("Disallow a blank line after 'use strict'; statements");
    disallowPaddingNewLinesBeforeExport.setDescription("Disallows newline before module.exports");
    disallowPaddingNewlinesBeforeKeywords.setDescription("Disallow an empty line above the specified keywords.");
    disallowPaddingNewLinesBeforeLineComments.setDescription("Disallows newline before line comments");
    disallowPaddingNewlinesInBlocks.setDescription("Disallows blocks from beginning or ending with 2 newlines.");
    disallowPaddingNewLinesInObjects.setDescription("Disallows newline inside curly braces of all objects.");
    disallowQuotedKeysInObjects.setDescription("Disallows quoted keys in object if possible.");
    disallowSemicolons.setDescription("Disallows lines from ending in a semicolon.");
    disallowSpaceAfterBinaryOperators.setDescription("Requires sticking binary operators to the right.");
    disallowSpaceAfterKeywords.setDescription("Disallows space after keyword.");
    disallowSpaceAfterLineComment.setDescription("Requires that a line comment (//) not be followed by a space.");
    disallowSpaceAfterObjectKeys.setDescription("Disallows space after object keys.");
    disallowSpaceAfterPrefixUnaryOperators.setDescription("Requires sticking unary operators to the right.");
    disallowSpaceBeforeBinaryOperators.setDescription("Requires sticking binary operators to the left.");
    disallowSpaceBeforeBlockStatements.setDescription("Disallows space before block statements (for loops, control structures).");
    disallowSpaceBeforeKeywords.setDescription("Disallows space before keyword.");
    disallowSpaceBeforeObjectValues.setDescription("Disallows space after object keys.");
    disallowSpaceBeforePostfixUnaryOperators.setDescription("Requires sticking unary operators to the left.");
    disallowSpaceBetweenArguments.setDescription("Ensure there are no spaces after argument separators in call expressions.");
    disallowSpacesInAnonymousFunctionExpression.setDescription("Disallows space before () or {} in anonymous function expressions.");
    disallowSpacesInCallExpression.setDescription("Disallows space before () in call expressions.");
    disallowSpacesInConditionalExpression.setDescription("Disallows space before and/or after ? or : in conditional expressions.");
    disallowSpacesInForStatement.setDescription("Disallow spaces in between for statement.");
    disallowSpacesInFunctionDeclaration.setDescription("Disallows space before () or {} in function declarations.");
    disallowSpacesInFunctionExpression.setDescription("Disallows space before () or {} in function expressions (both named and anonymous).");
    disallowSpacesInFunction.setDescription("Expression");
    disallowSpacesInNamedFunctionExpression.setDescription("Disallows space before () or {} in named function expressions.");
    disallowSpacesInsideArrayBrackets.setDescription("Disallows space after opening array square bracket and before closing.");
    disallowSpacesInsideBrackets.setDescription("Disallows space after opening square bracket and before closing.");
    disallowSpacesInsideObjectBrackets.setDescription("Disallows space after opening object curly brace and before closing.");
    disallowSpacesInsideParentheses.setDescription("Disallows space after opening round bracket and before closing.");
    disallowSpaceBeforeComma.setDescription("Disallows spaces before comma");
    requireSpaceBeforeComma.setDescription("Requires space before comma");
    disallowSpaceBeforeSemicolon.setDescription("Disallows spaces before semicolons");
    disallowParenthesesAroundArrowParam.setDescription("Disallows parentheses around arrow function expressions with a single parameter");
    requireParenthesesAroundArrowParam.setDescription("Requires parentheses around arrow function expressions with a single parameter");
    disallowObjectKeysOnNewLine.setDescription("Disallows placing object keys on new line");
    requireObjectKeysOnNewLine.setDescription("Requires placing object keys on new line");
    disallowTrailingComma.setDescription("Disallows an extra comma following the final element of an array or object literal.");
    disallowTrailingWhitespace.setDescription("Requires all lines to end on a non-whitespace character");
    disallowYodaConditions.setDescription("Requires the variable to be the left hand operator when doing a boolean comparison");
    maximumLineLength.setDescription("Requires all lines to be at most the number of characters specified");
    maximumNumberOfLines.setDescription("Requires the file to be at most the number of lines specified");
    requireAlignedObjectValues.setDescription("Requires proper alignment in object literals.");
    requireAnonymousFunctions.setDescription("Requires that a function expression be anonymous.");
    requireBlocksOnNewline.setDescription("Requires blocks to begin and end with a newline");
    requireCamelCaseOrUpperCaseIdentifiers.setDescription("Requires identifiers to be camelCased or UPPERCASE_WITH_UNDERSCORES");
    requireCapitalizedComments.setDescription("Requires the first alphabetical character of a comment to be uppercase, unless it is part of a multi-line textblock.");
    requireCapitalizedConstructors.setDescription("Requires constructors to be capitalized (except for this)");
    requireCommaBeforeLineBreak.setDescription("Requires commas as last token on a line in lists.");
    requireCurlyBraces.setDescription("Requires curly braces after statements.");
    requireDollarBeforejQueryAssignment.setDescription("Require a $ before variable names that are jquery assignments.");
    requireDotNotation.setDescription("Requires member expressions to use dot notation when possible");
    requireFunctionDeclarations.setDescription("Requires function declarations by disallowing assignment of functions expressions to variables. Function expressions are allowed in all other contexts, including when passed as function arguments or immediately invoked.");
    requireKeywordsOnNewLine.setDescription("Requires placing keywords on a new line.");
    requireLineBreakAfterVariableAssignment.setDescription("Requires placing line feed after assigning a variable.");
    requireLineFeedAtFileEnd.setDescription("Requires placing line feed at file end.");
    requireMultipleVarDecl.setDescription("Requires multiple var declaration.");
    requireNamedUnassignedFunctions.setDescription("Require unassigned functions to be named inline");
    requireNewlineBeforeBlockStatements.setDescription("Requires newline before opening curly brace of all block statements.");
    requireOperatorBeforeLineBreak.setDescription("Requires operators to appear before line breaks and not after.");
    requirePaddingNewLineAfterVariableDeclaration.setDescription("Requires an extra blank newline after var declarations, as long as it is not the last expression in the current block.");
    requirePaddingNewLinesAfterBlocks.setDescription("Requires newline after blocks");
    requirePaddingNewLinesAfterUseStrict.setDescription("Requires a blank line after 'use strict'; statements");
    requirePaddingNewLinesBeforeExport.setDescription("Requires newline before module.exports");
    requirePaddingNewlinesBeforeKeywords.setDescription("Requires an empty line above the specified keywords unless the keyword is the first expression in a block.");
    requirePaddingNewLinesBeforeLineComments.setDescription("Requires newline before line comments");
    requirePaddingNewlinesInBlocks.setDescription("Requires blocks to begin and end with 2 newlines");
    requirePaddingNewLinesInObjects.setDescription("Requires newline inside curly braces of all objects.");
    requireParenthesesAroundIIFE.setDescription("Requires parentheses around immediately invoked function expressions.");
    requireQuotedKeysInObjects.setDescription("Requires quoted keys in objects.");
    requireSemicolons.setDescription("Requires semicolon after:");
    requireSpaceAfterBinaryOperators.setDescription("Disallows sticking binary operators to the right.");
    requireSpaceAfterKeywords.setDescription("Requires space after keyword.");
    requireSpaceAfterLineComment.setDescription("Requires that a line comment (//) be followed by a space.");
    requireSpaceAfterObjectKeys.setDescription("Requires space after object keys.");
    requireSpaceAfterPrefixUnaryOperators.setDescription("Disallows sticking unary operators to the right.");
    requireSpaceBeforeBinaryOperators.setDescription("Disallows sticking binary operators to the left.");
    requireSpaceBeforeBlockStatements.setDescription("Requires space(s) before block statements (for loops, control structures).");
    requireSpaceBeforeKeywords.setDescription("Requires space before keyword.");
    requireSpaceBeforeObjectValues.setDescription("Requires space after object keys.");
    requireSpaceBeforePostfixUnaryOperators.setDescription("Disallows sticking unary operators to the left.");
    requireSpaceBetweenArguments.setDescription("Ensure there are spaces after argument separators in call expressions.");
    requireSpacesInAnonymousFunctionExpression.setDescription("Requires space before () or {} in anonymous function expressions.");
    requireSpacesInCallExpression.setDescription("Requires space before () in call expressions.");
    requireSpacesInConditionalExpression.setDescription("Requires space before and/or after ? or : in conditional expressions.");
    requireSpacesInForStatement.setDescription("Requires spaces inbetween for statement.");
    requireSpacesInFunctionDeclaration.setDescription("Requires space before () or {} in function declarations.");
    requireSpacesInFunctionExpression.setDescription("Requires space before () or {} in function expressions (both named and anonymous).");
    requireSpacesInFunction.setDescription("Expression");
    requireSpacesInNamedFunctionExpression.setDescription("Requires space before () or {} in named function expressions.");
    requireSpacesInsideArrayBrackets.setDescription("Requires space after opening array square bracket and before closing.");
    requireSpacesInsideBrackets.setDescription("Requires space after opening square bracket and before closing.");
    requireSpacesInsideObjectBrackets.setDescription("Requires space after opening object curly brace and before closing.");
    requireSpacesInsideParentheses.setDescription("Requires space after opening round bracket and before closing.");
    requireTrailingComma.setDescription("Requires an extra comma following the final element of an array or object literal.");
    requireYodaConditions.setDescription("Requires the variable to be the right hand operator when doing a boolean comparison");
    requireVarDeclFirst.setDescription("Requires var declaration to be on the top of an enclosing scope");
    requireMatchingFunctionName.setDescription("Requires function names to match member and property names");
    requireTemplateStrings.setDescription("Requires the use of template strings instead of string concatenation");
    requireSpread.setDescription("Disallows using .apply in favor of the spread operator");
    requireShorthandArrowFunctions.setDescription("Require arrow functions to use an expression body when returning a single statement (no block statement, implicit return)");
    requireArrowFunctions.setDescription("Requires that arrow functions are used instead of anonymous function expressions in callbacks");
    disallowNodeTypes.setDescription("Disallow use of certain node types (from Esprima/ESTree). Esprima node types");
    requireNumericLiterals.setDescription("Requires use of binary, hexadecimal, and octal literals instead of parseInt");
    safeContextKeyword.setDescription("Option to check var that = this expressions");
    validateAlignedFunctionParameters.setDescription("Validates proper alignment of function parameters.");
    validateIndentation.setDescription("Validates indentation for switch statements and block statements");
    validateLineBreaks.setDescription("Option to check line break characters");
    validateNewlineAfterArrayElements.setDescription("Requires each element in array on a single line when array length is more than passed maximum number or array fills more than one line. Set ignoreBrackets to true to allow elements on the same line with brackets.");
    validateParameterSeparator.setDescription("Enable validation of separators between function parameters. Will ignore newlines.");
    validateQuoteMarks.setDescription("Requires all quote marks to be either the supplied value, or consistent if true");
    jsDoc.setDescription("Validate jsdoc comments");

    disallowShorthandArrowFunctions.setDescription("Require arrow functions to use a block statement (explicit return)");
    disallowArrowFunctions.setDescription("Disallows arrow functions");
    validateOrderInObjectKeys.setDescription("Validates the order in object keys");
    disallowEmptyBlocks.setDescription("Disallows empty blocks (except for catch blocks)");

    //02/2016
    disallowNestedTernaries.setDescription("Disallows nested ternaries");
    disallowMultiLineTernary.setDescription("Disallows the test, consequent and alternate to be on separate lines when using the ternary operator");
    disallowIdenticalDestructuringNames.setDescription("Disallows identical destructuring names for the key and value in favor of using shorthand destructuring");
    disallowSpaceAfterComma.setDescription("Disallows spaces after commas");
    disallowSpacesInGenerator.setDescription("Disallow space before or after * in generator functions");
    disallowSpacesInsideParenthesizedExpression.setDescription("Disallows space after opening and before closing grouping parentheses.");
    disallowSpacesInsideTemplateStringPlaceholders.setDescription("Disallows spaces before and after curly brace inside template string placeholders.");
    disallowTabs.setDescription("Disallows tabs everywhere.");
    disallowUnusedParams.setDescription("Disallows unused params in function expression and function declaration.");
    disallowVar.setDescription("Disallows declaring variables with var.");
    requireAlignedMultilineParams.setDescription("Enforces indentation of parameters in multiline functions");
    requireArrayDestructuring.setDescription("Requires that variable assignment from array values are * destructured.");
    requireCapitalizedConstructorsNew.setDescription("Requires capitalized constructors to to use the new keyword");
    requireEarlyReturn.setDescription("Requires to return early in a function.");
    requireEnhancedObjectLiterals.setDescription("Requires declaring objects via ES6 enhanced object literals");
    requireImportAlphabetized.setDescription("Requires imports to be alphabetised");
    requireMultiLineTernary.setDescription("Requires the test, consequent and alternate to be on separate lines when using the ternary operator.");
    requireObjectDestructuring.setDescription("Requires variable declarations from objects via destructuring");
    requireSpaceAfterComma.setDescription("Requires space after comma");
    requireSpacesInGenerator.setDescription("Requires space before and after * in generator functions");
    requireSpacesInsideParenthesizedExpression.setDescription("Requires space after opening and before closing grouping parentheses.");
    validateCommentPosition.setDescription("This rule is for validating the positioning of line comments. Block comments are ignored.");
    disallowArrayDestructuringReturn.setDescription("Requires object destructuring for multiple return values, not array destructuring.");
    disallowSpacesInsideImportedObjectBraces.setDescription("Disallow space after opening object curly brace and before closing in import statements.");
    disallowUnusedVariables.setDescription("Disallows unused variables defined with var, let or const.");
    requireNewlineBeforeSingleStatementsInIf.setDescription("Requires newline before single if statements.");
    requireSpaceBeforeDestructuredValues.setDescription("Require space after colon in object destructuring.");
    requireSpacesInsideImportedObjectBraces.setDescription("Requires space after opening object curly brace and before closing in import statements.");
    requireUseStrict.setDescription("Requires 'use strict'; statements.");

    additionalRules.setDescription("Path to load additional rules");
    preset.setDescription("Extends defined rules with preset rules.");
    excludeFiles.setDescription("Disables style checking for specified paths.");
    fileExtensions.setDescription("Changes the set of file extensions that will be processed.");

    extract.setDescription("Set list of glob patterns for files which embedded JavaScript should be checked");
    maxErrors.setDescription("Set the maximum number of errors to report");
    es3.setDescription("Use ES3 reserved words");
    errorFilter.setDescription("A filter function that determines whether or not to report an error. This will be called for every found error.");
    plugins.setDescription("Paths to load plugins. See the wiki page for more details about the Plugin API");
  }

  private static JscsOptionDescriptor beforeBraces() {
    return descr().addType(obj, new JscsOptionDescriptor.ValuesObject((o)-> o.addBoolFields("beforeOpeningRoundBrace", "beforeOpeningCurlyBrace")));
  }

  private static JscsOptionDescriptor ternary() {
    return descr().canBool().addType(obj, new JscsOptionDescriptor.ValuesObject((o)-> o.addBoolFields("afterTest", "beforeConsequent", "afterConsequent", "beforeAlternate")));
  }

  private static JscsOptionDescriptor boolOrKeywordsArray() {
    return descr().canBool().canKeywordsArray();
  }

  private static JscsOptionDescriptor descr() {
    return new JscsOptionDescriptor();
  }

  @Nullable
  public static JscsOption safeValueOf(@NotNull final String name) {
    try {
      return valueOf(name);
    }
    catch (IllegalArgumentException e) {
      return null;
    }
  }

  private final JscsOptionDescriptor myDescriptor;
  private String myDescription;
  private final List<ValueType> myTypesList;

  JscsOption(JscsOptionDescriptor descriptor) {
    myDescriptor = descriptor;
    myTypesList = new ArrayList<>(myDescriptor.getTypes().keySet());
    Collections.sort(myTypesList);
  }

  public JscsOptionDescriptor getDescriptor() {
    return myDescriptor;
  }

  public String getDescription() {
    return myDescription;
  }

  public void setDescription(String description) {
    myDescription = description;
  }

  public boolean canBe(ValueType type) {
    return myDescriptor.getTypes().containsKey(type);
  }

  public List<ValueType> getTypesList() {
    return myTypesList;
  }

  @NotNull
  public List<ValueType> otherTypes(@Nullable ValueType type) {
    if (type == null) return Collections.unmodifiableList(myTypesList);
    if (myTypesList.size() == 1) return Collections.emptyList();

    final List<ValueType> list = new ArrayList<>(myTypesList);
    list.remove(type);
    return list;
  }
}
