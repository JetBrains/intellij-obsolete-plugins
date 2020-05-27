package com.intellij.lang.javascript.linter.jscs.config;

/**
 * @author Irina.Chernushina on 4/30/2015.
 */
public class Constants {
  public static final String[] keywords = {"break", "default", "function", "return", "var", "case", "delete", "if", "switch",
    "void", "catch", "do", "in", "this", "while", "const", "else", "instanceof",
    "throw", "with", "continue", "finally", "let", "try", "debugger", "for", "new", "typeof"};
  public static final String[] binaryOperators = {"=", ",", "+", "-", "/", "*", "%",
    "==", "===", "!=", "!==", ">", ">=", "<", "<=",
    "&", "|", "^", "<<", ">>", ">>>",
    "+=", "-=", "*=", "/=", "%=", "&=", "|=", "^=", ">>=", "<<=", ">>>=",
    "&&", "||", ".", "::", "**"};
  public static final String[] operators = {"=", ",", "+", "-", "/", "*", "%",
    "==", "===", "!=", "!==", ">", ">=", "<", "<=",
    "&", "|", "^", "<<", ">>", ">>>",
    "+=", "-=", "*=", "/=", "%=", "&=", "|=", "^=", ">>=", "<<=", ">>>=",
    "&&", "||",
    "++", "--", "+", "-", "~", "!",
    "?", ".", "::", "**"};
  public static final String[] unaryOperators = {"++", "--", "+", "-", "~", "!", "::"};
  public static final String[] conditionalOperators = {"==", "===", "!=", "!==", ">", ">=", "<", "<=", "&&", "||"};
  public static final String[] blockStatementKeywords = {"if", "else", "try", "catch", "finally", "do", "while", "for", "function"};
}
