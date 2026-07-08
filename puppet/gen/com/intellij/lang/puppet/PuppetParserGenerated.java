// This is a generated file. Not intended for manual editing.
package com.intellij.lang.puppet;

import com.intellij.lang.PsiBuilder;
import com.intellij.lang.PsiBuilder.Marker;
import static com.intellij.lang.puppet.PuppetTokenTypes.*;
import static com.intellij.lang.puppet.grammar.PuppetParserUtil.*;
import com.intellij.psi.tree.IElementType;
import com.intellij.lang.ASTNode;
import com.intellij.psi.tree.TokenSet;
import com.intellij.lang.PsiParser;
import com.intellij.lang.LightPsiParser;
import static com.intellij.lang.WhitespacesBinders.*;

@SuppressWarnings({"SimplifiableIfStatement", "UnusedAssignment"})
public class PuppetParserGenerated implements PsiParser, LightPsiParser {

  public ASTNode parse(IElementType t, PsiBuilder b) {
    parseLight(t, b);
    return b.getTreeBuilt();
  }

  public void parseLight(IElementType t, PsiBuilder b) {
    boolean r;
    b = adapt_builder_(t, b, this, EXTENDS_SETS_);
    Marker m = enter_section_(b, 0, _COLLAPSE_, null);
    r = parse_root_(t, b);
    exit_section_(b, 0, m, t, r, true, TRUE_CONDITION);
  }

  protected boolean parse_root_(IElementType t, PsiBuilder b) {
    return parse_root_(t, b, 0);
  }

  static boolean parse_root_(IElementType t, PsiBuilder b, int l) {
    return puppetFile(b, l + 1);
  }

  public static final TokenSet[] EXTENDS_SETS_ = new TokenSet[] {
    create_token_set_(DEFAULT_SELECTOR_VALUE, SELECTOR_VALUE),
    create_token_set_(BRACED_ANY_ARGUMENTS_LIST_BLOCK, BRACED_CASE_OPTS_BLOCK, BRACED_RESOURCE_BY_CLASSNAME_CONTENTS_BLOCK, BRACED_SELECTOR_VALUES_BLOCK,
      BRACED_STATEMENTS_BLOCK, DATA_TYPE_PARAMETERS_BLOCK, INTERPOLATED_BLOCK, PARENTHESIZED_EXPRESSIONS_LIST_BLOCK,
      PARENTHESIZED_PARAMETERS_LIST_BLOCK, PIPED_PARAMETERS_LIST_BLOCK, RESOURCE_LIKE_CLASS_DECLARATION_BLOCK),
    create_token_set_(ANY_NAME_WRAPPER, ARRAY, CAPITALIZED_NAME_WRAPPER, CASE_EXPRESSION,
      CLASS_DEFINITION, COLLECTION, CONSUMES_STATEMENT, DATA_TYPE,
      DEFAULT_WRAPPER, EXPRESSION, EXPRESSION_ADDITIVE, EXPRESSION_ASSIGNMENT,
      EXPRESSION_BINARY, EXPRESSION_CALL, EXPRESSION_COMP, EXPRESSION_FALSE,
      EXPRESSION_IN, EXPRESSION_INDEX, EXPRESSION_INDEXED, EXPRESSION_MULTIPLICATIVE,
      EXPRESSION_NUMERIC, EXPRESSION_PAREN, EXPRESSION_REGEX, EXPRESSION_RELATION,
      EXPRESSION_TRUE, EXPRESSION_UNARY, EXPRESSION_UNDEF, FUNCTION_CALL_EXPRESSION,
      FUNCTION_DEFINITION, HASH_ARRAY_ACCESSES, HASH_VALUE, HEREDOC_EXPRESSION,
      IF_STATEMENT, IMPORT_STATEMENT, INCLUDE_CLASS_EXPRESSION, NODE_DEFINITION,
      PRODUCES_STATEMENT, QUOTED_TEXT, REGEXP, REGULAR_NAME_WRAPPER,
      RESOURCE_DECLARATION, RESOURCE_DEFAULT_STATEMENT, SELECTOR, SITE_COMPOUND,
      TYPE_DEFINITION, UNLESS_STATEMENT, VAR_WRAPPER),
  };

  /* ********************************************************** */
  // <<isPuppet4>> piped_parameters_list_block braced_statements_block
  public static boolean anonymousBlock(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "anonymousBlock")) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_, ANONYMOUS_BLOCK, "<anonymous block>");
    r = isPuppet4(b, l + 1);
    r = r && piped_parameters_list_block(b, l + 1);
    p = r; // pin = 2
    r = r && braced_statements_block(b, l + 1);
    exit_section_(b, l, m, r, p, null);
    return r || p;
  }

  /* ********************************************************** */
  // regular_name_wrapper | capitalized_name_wrapper
  public static boolean any_name_wrapper(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "any_name_wrapper")) return false;
    if (!nextTokenIs(b, "<any name wrapper>", CAPITALIZED_NAME, NAME)) return false;
    boolean r;
    Marker m = enter_section_(b, l, _COLLAPSE_, ANY_NAME_WRAPPER, "<any name wrapper>");
    r = regular_name_wrapper(b, l + 1);
    if (!r) r = capitalized_name_wrapper(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  /* ********************************************************** */
  // argument_name {'=>'|'+>'} expression
  public static boolean argument(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "argument")) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_, ARGUMENT, "<argument>");
    r = argument_name(b, l + 1);
    p = r; // pin = 1
    r = r && report_error_(b, argument_1(b, l + 1));
    r = p && expression(b, l + 1, -1) && r;
    exit_section_(b, l, m, r, p, PuppetParserGenerated::paramRecoverWhile);
    return r || p;
  }

  // '=>'|'+>'
  private static boolean argument_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "argument_1")) return false;
    boolean r;
    r = consumeToken(b, FARROW);
    if (!r) r = consumeToken(b, PARROW);
    return r;
  }

  /* ********************************************************** */
  // regular_name_wrapper
  //   | keyword
  //   | BOOLEAN
  static boolean argument_name(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "argument_name")) return false;
    boolean r;
    r = regular_name_wrapper(b, l + 1);
    if (!r) r = keyword(b, l + 1);
    if (!r) r = consumeToken(b, BOOLEAN);
    return r;
  }

  /* ********************************************************** */
  // set_argument (',' <<passHeredocBodies>>? (set_argument | &'}' | &';'))*
  static boolean arguments_list(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "arguments_list")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_);
    r = set_argument(b, l + 1);
    r = r && arguments_list_1(b, l + 1);
    exit_section_(b, l, m, r, false, PuppetParserGenerated::paramListRecover);
    return r;
  }

  // (',' <<passHeredocBodies>>? (set_argument | &'}' | &';'))*
  private static boolean arguments_list_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "arguments_list_1")) return false;
    while (true) {
      int c = current_position_(b);
      if (!arguments_list_1_0(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "arguments_list_1", c)) break;
    }
    return true;
  }

  // ',' <<passHeredocBodies>>? (set_argument | &'}' | &';')
  private static boolean arguments_list_1_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "arguments_list_1_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, COMMA);
    r = r && arguments_list_1_0_1(b, l + 1);
    r = r && arguments_list_1_0_2(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // <<passHeredocBodies>>?
  private static boolean arguments_list_1_0_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "arguments_list_1_0_1")) return false;
    passHeredocBodies(b, l + 1);
    return true;
  }

  // set_argument | &'}' | &';'
  private static boolean arguments_list_1_0_2(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "arguments_list_1_0_2")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = set_argument(b, l + 1);
    if (!r) r = arguments_list_1_0_2_1(b, l + 1);
    if (!r) r = arguments_list_1_0_2_2(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // &'}'
  private static boolean arguments_list_1_0_2_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "arguments_list_1_0_2_1")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _AND_);
    r = consumeToken(b, RBRACE);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  // &';'
  private static boolean arguments_list_1_0_2_2(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "arguments_list_1_0_2_2")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _AND_);
    r = consumeToken(b, SEMIC);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  /* ********************************************************** */
  // '!=' | '==' | '>' | '<' | '>=' | '<='
  static boolean binaryOp0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "binaryOp0")) return false;
    boolean r;
    r = consumeToken(b, NOTEQUAL);
    if (!r) r = consumeToken(b, ISEQUAL);
    if (!r) r = consumeToken(b, GREATERTHAN);
    if (!r) r = consumeToken(b, LESSTHAN);
    if (!r) r = consumeToken(b, GREATEREQUAL);
    if (!r) r = consumeToken(b, LESSEQUAL);
    return r;
  }

  /* ********************************************************** */
  // 'in'
  static boolean binaryOp1(PsiBuilder b, int l) {
    return consumeToken(b, IN);
  }

  /* ********************************************************** */
  // '+' | '-'
  static boolean binaryOp2(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "binaryOp2")) return false;
    if (!nextTokenIs(b, "", MINUS, PLUS)) return false;
    boolean r;
    r = consumeToken(b, PLUS);
    if (!r) r = consumeToken(b, MINUS);
    return r;
  }

  /* ********************************************************** */
  // '/' | '*' | '%'
  static boolean binaryOp3(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "binaryOp3")) return false;
    boolean r;
    r = consumeToken(b, DIV);
    if (!r) r = consumeToken(b, TIMES);
    if (!r) r = consumeToken(b, MODULO);
    return r;
  }

  /* ********************************************************** */
  // '<<' | '>>' | 'and' | 'or'
  static boolean binaryOp4(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "binaryOp4")) return false;
    boolean r;
    r = consumeToken(b, LSHIFT);
    if (!r) r = consumeToken(b, RSHIFT);
    if (!r) r = consumeToken(b, AND);
    if (!r) r = consumeToken(b, OR);
    return r;
  }

  /* ********************************************************** */
  // <<braced_element resource_arguments_list?>>
  public static boolean braced_any_arguments_list_block(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "braced_any_arguments_list_block")) return false;
    if (!nextTokenIs(b, LBRACE)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = braced_element(b, l + 1, PuppetParserGenerated::braced_any_arguments_list_block_0_0);
    exit_section_(b, m, BRACED_ANY_ARGUMENTS_LIST_BLOCK, r);
    return r;
  }

  // resource_arguments_list?
  private static boolean braced_any_arguments_list_block_0_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "braced_any_arguments_list_block_0_0")) return false;
    resource_arguments_list(b, l + 1);
    return true;
  }

  /* ********************************************************** */
  // <<braced_element case_options>>
  public static boolean braced_case_opts_block(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "braced_case_opts_block")) return false;
    if (!nextTokenIs(b, LBRACE)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = braced_element(b, l + 1, PuppetParserGenerated::case_options);
    exit_section_(b, m, BRACED_CASE_OPTS_BLOCK, r);
    return r;
  }

  /* ********************************************************** */
  // '{' <<braced_element_content <<x1>>>> '}'
  static boolean braced_element(PsiBuilder b, int l, Parser _x1) {
    if (!recursion_guard_(b, l, "braced_element")) return false;
    if (!nextTokenIs(b, LBRACE)) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_);
    r = consumeToken(b, LBRACE);
    p = r; // pin = 1
    r = r && report_error_(b, braced_element_content(b, l + 1, _x1));
    r = p && consumeToken(b, RBRACE) && r;
    exit_section_(b, l, m, r, p, null);
    return r || p;
  }

  /* ********************************************************** */
  // <<x1>>
  static boolean braced_element_content(PsiBuilder b, int l, Parser _x1) {
    if (!recursion_guard_(b, l, "braced_element_content")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_);
    r = _x1.parse(b, l);
    exit_section_(b, l, m, r, false, PuppetParserGenerated::recoverBraced);
    return r;
  }

  /* ********************************************************** */
  // <<braced_element <<resource_like_declarations_list resource_instance_declaration>>>>
  public static boolean braced_resource_by_classname_contents_block(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "braced_resource_by_classname_contents_block")) return false;
    if (!nextTokenIs(b, LBRACE)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = braced_element(b, l + 1, braced_resource_by_classname_contents_block_0_0_parser_);
    exit_section_(b, m, BRACED_RESOURCE_BY_CLASSNAME_CONTENTS_BLOCK, r);
    return r;
  }

  /* ********************************************************** */
  // selector_value | default_selector_value
  static boolean braced_selector_value_element(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "braced_selector_value_element")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_);
    r = selector_value(b, l + 1);
    if (!r) r = default_selector_value(b, l + 1);
    exit_section_(b, l, m, r, false, PuppetParserGenerated::recover_braced_selector_value);
    return r;
  }

  /* ********************************************************** */
  // <<braced_element selector_values>>
  public static boolean braced_selector_values_block(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "braced_selector_values_block")) return false;
    if (!nextTokenIs(b, LBRACE)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = braced_element(b, l + 1, PuppetParserGenerated::selector_values);
    exit_section_(b, m, BRACED_SELECTOR_VALUES_BLOCK, r);
    return r;
  }

  /* ********************************************************** */
  // <<braced_element statements?>>
  public static boolean braced_statements_block(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "braced_statements_block")) return false;
    if (!nextTokenIs(b, LBRACE)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = braced_element(b, l + 1, PuppetParserGenerated::braced_statements_block_0_0);
    exit_section_(b, m, BRACED_STATEMENTS_BLOCK, r);
    return r;
  }

  // statements?
  private static boolean braced_statements_block_0_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "braced_statements_block_0_0")) return false;
    statements(b, l + 1);
    return true;
  }

  /* ********************************************************** */
  // '[' <<bracketed_element_content <<x1>>>> ']'
  static boolean bracketed_element(PsiBuilder b, int l, Parser _x1) {
    if (!recursion_guard_(b, l, "bracketed_element")) return false;
    if (!nextTokenIs(b, LBRACK)) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_);
    r = consumeToken(b, LBRACK);
    p = r; // pin = 1
    r = r && report_error_(b, bracketed_element_content(b, l + 1, _x1));
    r = p && consumeToken(b, RBRACK) && r;
    exit_section_(b, l, m, r, p, null);
    return r || p;
  }

  /* ********************************************************** */
  // <<x1>>
  static boolean bracketed_element_content(PsiBuilder b, int l, Parser _x1) {
    if (!recursion_guard_(b, l, "bracketed_element_content")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_);
    r = _x1.parse(b, l);
    exit_section_(b, l, m, r, false, PuppetParserGenerated::recoverBracketed);
    return r;
  }

  /* ********************************************************** */
  // CAPITALIZED_NAME
  public static boolean capitalized_name_wrapper(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "capitalized_name_wrapper")) return false;
    if (!nextTokenIs(b, CAPITALIZED_NAME)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, CAPITALIZED_NAME);
    exit_section_(b, m, CAPITALIZED_NAME_WRAPPER, r);
    return r;
  }

  /* ********************************************************** */
  // expressions_list | default_wrapper
  public static boolean caseValues(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "caseValues")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, CASE_VALUES, "<case values>");
    r = expressions_list(b, l + 1);
    if (!r) r = default_wrapper(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  /* ********************************************************** */
  // caseValues ':' braced_statements_block
  public static boolean case_option(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "case_option")) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_, CASE_OPTION, "<case option>");
    r = caseValues(b, l + 1);
    p = r; // pin = 1
    r = r && report_error_(b, consumeToken(b, COLON));
    r = p && braced_statements_block(b, l + 1) && r;
    exit_section_(b, l, m, r, p, PuppetParserGenerated::recover_case_option);
    return r || p;
  }

  /* ********************************************************** */
  // case_option+
  static boolean case_options(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "case_options")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = case_option(b, l + 1);
    while (r) {
      int c = current_position_(b);
      if (!case_option(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "case_options", c)) break;
    }
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // class_name_or_rvalue (',' class_name_or_rvalue)*
  static boolean classNameList(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "classNameList")) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_);
    r = class_name_or_rvalue(b, l + 1);
    p = r; // pin = 1
    r = r && classNameList_1(b, l + 1);
    exit_section_(b, l, m, r, p, null);
    return r || p;
  }

  // (',' class_name_or_rvalue)*
  private static boolean classNameList_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "classNameList_1")) return false;
    while (true) {
      int c = current_position_(b);
      if (!classNameList_1_0(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "classNameList_1", c)) break;
    }
    return true;
  }

  // ',' class_name_or_rvalue
  private static boolean classNameList_1_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "classNameList_1_0")) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_);
    r = consumeToken(b, COMMA);
    p = r; // pin = 1
    r = r && class_name_or_rvalue(b, l + 1);
    exit_section_(b, l, m, r, p, null);
    return r || p;
  }

  /* ********************************************************** */
  // INHERITS (any_name_wrapper | default_wrapper)
  static boolean class_inheritance(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "class_inheritance")) return false;
    if (!nextTokenIs(b, INHERITS)) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_);
    r = consumeToken(b, INHERITS);
    p = r; // pin = 1
    r = r && class_inheritance_1(b, l + 1);
    exit_section_(b, l, m, r, p, null);
    return r || p;
  }

  // any_name_wrapper | default_wrapper
  private static boolean class_inheritance_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "class_inheritance_1")) return false;
    boolean r;
    r = any_name_wrapper(b, l + 1);
    if (!r) r = default_wrapper(b, l + 1);
    return r;
  }

  /* ********************************************************** */
  // regular_name_wrapper | terminal
  static boolean class_name_or_rvalue(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "class_name_or_rvalue")) return false;
    boolean r;
    r = regular_name_wrapper(b, l + 1);
    if (!r) r = expression(b, l + 1, 11);
    return r;
  }

  /* ********************************************************** */
  // collLVal ('==' | '!=') expression
  public static boolean collExpr(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "collExpr")) return false;
    if (!nextTokenIs(b, "<coll expr>", DOLLAR, NAME)) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, COLL_EXPR, "<coll expr>");
    r = collLVal(b, l + 1);
    r = r && collExpr_1(b, l + 1);
    r = r && expression(b, l + 1, -1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  // '==' | '!='
  private static boolean collExpr_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "collExpr_1")) return false;
    boolean r;
    r = consumeToken(b, ISEQUAL);
    if (!r) r = consumeToken(b, NOTEQUAL);
    return r;
  }

  /* ********************************************************** */
  // AND | OR
  static boolean collJoin(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "collJoin")) return false;
    if (!nextTokenIs(b, "", AND, OR)) return false;
    boolean r;
    r = consumeToken(b, AND);
    if (!r) r = consumeToken(b, OR);
    return r;
  }

  /* ********************************************************** */
  // varWrapper | regular_name_wrapper
  static boolean collLVal(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "collLVal")) return false;
    if (!nextTokenIs(b, "", DOLLAR, NAME)) return false;
    boolean r;
    r = varWrapper(b, l + 1);
    if (!r) r = regular_name_wrapper(b, l + 1);
    return r;
  }

  /* ********************************************************** */
  // '<|' collectionStatementsList '|>'
  static boolean collectR1Hand(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "collectR1Hand")) return false;
    if (!nextTokenIs(b, LCOLLECT)) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_);
    r = consumeToken(b, LCOLLECT);
    p = r; // pin = 1
    r = r && report_error_(b, collectionStatementsList(b, l + 1));
    r = p && consumeToken(b, RCOLLECT) && r;
    exit_section_(b, l, m, r, p, null);
    return r || p;
  }

  /* ********************************************************** */
  // collectR1Hand | collectRRHand
  public static boolean collectRHand(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "collectRHand")) return false;
    if (!nextTokenIs(b, "<collect r hand>", LCOLLECT, LLCOLLECT)) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, COLLECT_R_HAND, "<collect r hand>");
    r = collectR1Hand(b, l + 1);
    if (!r) r = collectRRHand(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  /* ********************************************************** */
  // '<<|' collectionStatementsList '|>>'
  static boolean collectRRHand(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "collectRRHand")) return false;
    if (!nextTokenIs(b, LLCOLLECT)) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_);
    r = consumeToken(b, LLCOLLECT);
    p = r; // pin = 1
    r = r && report_error_(b, collectionStatementsList(b, l + 1));
    r = p && consumeToken(b, RRCOLLECT) && r;
    exit_section_(b, l, m, r, p, null);
    return r || p;
  }

  /* ********************************************************** */
  // '(' collectionStatementsList ')' | collExpr
  public static boolean collectionStatement(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "collectionStatement")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, COLLECTION_STATEMENT, "<collection statement>");
    r = collectionStatement_0(b, l + 1);
    if (!r) r = collExpr(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  // '(' collectionStatementsList ')'
  private static boolean collectionStatement_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "collectionStatement_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, LPAREN);
    r = r && collectionStatementsList(b, l + 1);
    r = r && consumeToken(b, RPAREN);
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // collectionStatement? (collJoin collectionStatement)*
  static boolean collectionStatementsList(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "collectionStatementsList")) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_);
    r = collectionStatementsList_0(b, l + 1);
    p = r; // pin = 1
    r = r && collectionStatementsList_1(b, l + 1);
    exit_section_(b, l, m, r, p, null);
    return r || p;
  }

  // collectionStatement?
  private static boolean collectionStatementsList_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "collectionStatementsList_0")) return false;
    collectionStatement(b, l + 1);
    return true;
  }

  // (collJoin collectionStatement)*
  private static boolean collectionStatementsList_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "collectionStatementsList_1")) return false;
    while (true) {
      int c = current_position_(b);
      if (!collectionStatementsList_1_0(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "collectionStatementsList_1", c)) break;
    }
    return true;
  }

  // collJoin collectionStatement
  private static boolean collectionStatementsList_1_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "collectionStatementsList_1_0")) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_);
    r = collJoin(b, l + 1);
    p = r; // pin = 1
    r = r && collectionStatement(b, l + 1);
    exit_section_(b, l, m, r, p, null);
    return r || p;
  }

  /* ********************************************************** */
  // CASE | CLASS | DEFAULT |FUNCTION | DEFINE | SITE | APPLICATION | IF | IMPORT | NODE | UNLESS | FALSE | TRUE | UNDEF
  //   | DOLLAR
  //   | '@' | HEREDOC_AT | '@@' | '}'
  //   | CAPITALIZED_NAME | NAME
  //   | DOUBLE_QUOTED_STRING | DOUBLE_QUOTED_STRING_START | SINGLE_QUOTED_STRING| FLOAT_LITERAL | INTEGER_LITERAL_WITHOUTQ
  static boolean common_recovery_tokens(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "common_recovery_tokens")) return false;
    boolean r;
    r = consumeToken(b, CASE);
    if (!r) r = consumeToken(b, CLASS);
    if (!r) r = consumeToken(b, DEFAULT);
    if (!r) r = consumeToken(b, FUNCTION);
    if (!r) r = consumeToken(b, DEFINE);
    if (!r) r = consumeToken(b, SITE);
    if (!r) r = consumeToken(b, APPLICATION);
    if (!r) r = consumeToken(b, IF);
    if (!r) r = consumeToken(b, IMPORT);
    if (!r) r = consumeToken(b, NODE);
    if (!r) r = consumeToken(b, UNLESS);
    if (!r) r = consumeToken(b, FALSE);
    if (!r) r = consumeToken(b, TRUE);
    if (!r) r = consumeToken(b, UNDEF);
    if (!r) r = consumeToken(b, DOLLAR);
    if (!r) r = consumeToken(b, AT);
    if (!r) r = consumeToken(b, HEREDOC_AT);
    if (!r) r = consumeToken(b, ATAT);
    if (!r) r = consumeToken(b, RBRACE);
    if (!r) r = consumeToken(b, CAPITALIZED_NAME);
    if (!r) r = consumeToken(b, NAME);
    if (!r) r = consumeToken(b, DOUBLE_QUOTED_STRING);
    if (!r) r = consumeToken(b, DOUBLE_QUOTED_STRING_START);
    if (!r) r = consumeToken(b, SINGLE_QUOTED_STRING);
    if (!r) r = consumeToken(b, FLOAT_LITERAL);
    if (!r) r = consumeToken(b, INTEGER_LITERAL_WITHOUTQ);
    return r;
  }

  /* ********************************************************** */
  // expression braced_statements_block
  public static boolean conditional_branch(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "conditional_branch")) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_, CONDITIONAL_BRANCH, "<conditional branch>");
    r = expression(b, l + 1, -1);
    p = r; // pin = 1
    r = r && braced_statements_block(b, l + 1);
    exit_section_(b, l, m, r, p, null);
    return r || p;
  }

  /* ********************************************************** */
  // <<bracketed_element expressions_list_with_tailing_comma>>
  public static boolean data_type_parameters_block(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "data_type_parameters_block")) return false;
    if (!nextTokenIs(b, LBRACK)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = bracketed_element(b, l + 1, PuppetParserGenerated::expressions_list_with_tailing_comma);
    exit_section_(b, m, DATA_TYPE_PARAMETERS_BLOCK, r);
    return r;
  }

  /* ********************************************************** */
  // default_wrapper '=>' expression
  public static boolean default_selector_value(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "default_selector_value")) return false;
    if (!nextTokenIs(b, DEFAULT)) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_, DEFAULT_SELECTOR_VALUE, null);
    r = default_wrapper(b, l + 1);
    p = r; // pin = 1
    r = r && report_error_(b, consumeToken(b, FARROW));
    r = p && expression(b, l + 1, -1) && r;
    exit_section_(b, l, m, r, p, null);
    return r || p;
  }

  /* ********************************************************** */
  // ELSE braced_statements_block
  static boolean else_branch(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "else_branch")) return false;
    if (!nextTokenIs(b, ELSE)) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_);
    r = consumeToken(b, ELSE);
    p = r; // pin = 1
    r = r && braced_statements_block(b, l + 1);
    exit_section_(b, l, m, r, p, null);
    return r || p;
  }

  /* ********************************************************** */
  // ELSIF conditional_branch
  static boolean elsif_branch(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "elsif_branch")) return false;
    if (!nextTokenIs(b, ELSIF)) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_);
    r = consumeToken(b, ELSIF);
    p = r; // pin = 1
    r = r && conditional_branch(b, l + 1);
    exit_section_(b, l, m, r, p, null);
    return r || p;
  }

  /* ********************************************************** */
  // expressionRelation (('=' | '+=' | '-=' ) expressionRelation)*
  public static boolean expressionAssignment(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "expressionAssignment")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _COLLAPSE_, EXPRESSION_ASSIGNMENT, "<expression assignment>");
    r = expressionRelation(b, l + 1);
    r = r && expressionAssignment_1(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  // (('=' | '+=' | '-=' ) expressionRelation)*
  private static boolean expressionAssignment_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "expressionAssignment_1")) return false;
    while (true) {
      int c = current_position_(b);
      if (!expressionAssignment_1_0(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "expressionAssignment_1", c)) break;
    }
    return true;
  }

  // ('=' | '+=' | '-=' ) expressionRelation
  private static boolean expressionAssignment_1_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "expressionAssignment_1_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = expressionAssignment_1_0_0(b, l + 1);
    r = r && expressionRelation(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // '=' | '+=' | '-='
  private static boolean expressionAssignment_1_0_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "expressionAssignment_1_0_0")) return false;
    boolean r;
    r = consumeToken(b, EQUALS);
    if (!r) r = consumeToken(b, APPENDS);
    if (!r) r = consumeToken(b, DELETES);
    return r;
  }

  /* ********************************************************** */
  // expressionAssignment (',' expressionAssignment)* ','?
  static boolean expressionAssignments(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "expressionAssignments")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = expressionAssignment(b, l + 1);
    r = r && expressionAssignments_1(b, l + 1);
    r = r && expressionAssignments_2(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // (',' expressionAssignment)*
  private static boolean expressionAssignments_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "expressionAssignments_1")) return false;
    while (true) {
      int c = current_position_(b);
      if (!expressionAssignments_1_0(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "expressionAssignments_1", c)) break;
    }
    return true;
  }

  // ',' expressionAssignment
  private static boolean expressionAssignments_1_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "expressionAssignments_1_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, COMMA);
    r = r && expressionAssignment(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // ','?
  private static boolean expressionAssignments_2(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "expressionAssignments_2")) return false;
    consumeToken(b, COMMA);
    return true;
  }

  /* ********************************************************** */
  // <<bracketed_element expression>>
  public static boolean expressionIndex(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "expressionIndex")) return false;
    if (!nextTokenIs(b, LBRACK)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = bracketed_element(b, l + 1, expression_parser_);
    exit_section_(b, m, EXPRESSION_INDEX, r);
    return r;
  }

  /* ********************************************************** */
  // resource {('<-' | '->' | '<~' | '~>') resource} *
  public static boolean expressionRelation(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "expressionRelation")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _COLLAPSE_, EXPRESSION_RELATION, "<expression relation>");
    r = resource(b, l + 1);
    r = r && expressionRelation_1(b, l + 1);
    exit_section_(b, l, m, r, false, PuppetParserGenerated::expression_relation_recover_while);
    return r;
  }

  // {('<-' | '->' | '<~' | '~>') resource} *
  private static boolean expressionRelation_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "expressionRelation_1")) return false;
    while (true) {
      int c = current_position_(b);
      if (!expressionRelation_1_0(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "expressionRelation_1", c)) break;
    }
    return true;
  }

  // ('<-' | '->' | '<~' | '~>') resource
  private static boolean expressionRelation_1_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "expressionRelation_1_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = expressionRelation_1_0_0(b, l + 1);
    r = r && resource(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // '<-' | '->' | '<~' | '~>'
  private static boolean expressionRelation_1_0_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "expressionRelation_1_0_0")) return false;
    boolean r;
    r = consumeToken(b, OUT_EDGE);
    if (!r) r = consumeToken(b, IN_EDGE);
    if (!r) r = consumeToken(b, OUT_EDGE_SUB);
    if (!r) r = consumeToken(b, IN_EDGE_SUB);
    return r;
  }

  /* ********************************************************** */
  // !(expression_assignment_recovery_tokens)
  static boolean expression_assignment_recover_while(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "expression_assignment_recover_while")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NOT_);
    r = !expression_assignment_recover_while_0(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  // (expression_assignment_recovery_tokens)
  private static boolean expression_assignment_recover_while_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "expression_assignment_recover_while_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = expression_assignment_recovery_tokens(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // ',' | top_level_recovery_tokens
  static boolean expression_assignment_recovery_tokens(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "expression_assignment_recovery_tokens")) return false;
    boolean r;
    r = consumeToken(b, COMMA);
    if (!r) r = top_level_recovery_tokens(b, l + 1);
    return r;
  }

  /* ********************************************************** */
  // !(expression_relation_recovery_tokens)
  static boolean expression_relation_recover_while(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "expression_relation_recover_while")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NOT_);
    r = !expression_relation_recover_while_0(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  // (expression_relation_recovery_tokens)
  private static boolean expression_relation_recover_while_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "expression_relation_recover_while_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = expression_relation_recovery_tokens(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // '=' | '+=' | '-=' | expression_assignment_recovery_tokens
  static boolean expression_relation_recovery_tokens(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "expression_relation_recovery_tokens")) return false;
    boolean r;
    r = consumeToken(b, EQUALS);
    if (!r) r = consumeToken(b, APPENDS);
    if (!r) r = consumeToken(b, DELETES);
    if (!r) r = expression_assignment_recovery_tokens(b, l + 1);
    return r;
  }

  /* ********************************************************** */
  // expression (',' expression)*
  static boolean expressions_list(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "expressions_list")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = expression(b, l + 1, -1);
    r = r && expressions_list_1(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // (',' expression)*
  private static boolean expressions_list_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "expressions_list_1")) return false;
    while (true) {
      int c = current_position_(b);
      if (!expressions_list_1_0(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "expressions_list_1", c)) break;
    }
    return true;
  }

  // ',' expression
  private static boolean expressions_list_1_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "expressions_list_1_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, COMMA);
    r = r && expression(b, l + 1, -1);
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // expression (',' (expression | &')' | &']'))*
  static boolean expressions_list_with_tailing_comma(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "expressions_list_with_tailing_comma")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = expression(b, l + 1, -1);
    r = r && expressions_list_with_tailing_comma_1(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // (',' (expression | &')' | &']'))*
  private static boolean expressions_list_with_tailing_comma_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "expressions_list_with_tailing_comma_1")) return false;
    while (true) {
      int c = current_position_(b);
      if (!expressions_list_with_tailing_comma_1_0(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "expressions_list_with_tailing_comma_1", c)) break;
    }
    return true;
  }

  // ',' (expression | &')' | &']')
  private static boolean expressions_list_with_tailing_comma_1_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "expressions_list_with_tailing_comma_1_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, COMMA);
    r = r && expressions_list_with_tailing_comma_1_0_1(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // expression | &')' | &']'
  private static boolean expressions_list_with_tailing_comma_1_0_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "expressions_list_with_tailing_comma_1_0_1")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = expression(b, l + 1, -1);
    if (!r) r = expressions_list_with_tailing_comma_1_0_1_1(b, l + 1);
    if (!r) r = expressions_list_with_tailing_comma_1_0_1_2(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // &')'
  private static boolean expressions_list_with_tailing_comma_1_0_1_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "expressions_list_with_tailing_comma_1_0_1_1")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _AND_);
    r = consumeToken(b, RPAREN);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  // &']'
  private static boolean expressions_list_with_tailing_comma_1_0_1_2(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "expressions_list_with_tailing_comma_1_0_1_2")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _AND_);
    r = consumeToken(b, RBRACK);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  /* ********************************************************** */
  // NAMESPACE_SEPARATOR ? (namespace_definition NAMESPACE_SEPARATOR)* NAME
  public static boolean fqn_container(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "fqn_container")) return false;
    if (!nextTokenIs(b, "<fqn container>", NAME, NAMESPACE_SEPARATOR)) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, FQN_CONTAINER, "<fqn container>");
    r = fqn_container_0(b, l + 1);
    r = r && fqn_container_1(b, l + 1);
    r = r && consumeToken(b, NAME);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  // NAMESPACE_SEPARATOR ?
  private static boolean fqn_container_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "fqn_container_0")) return false;
    consumeToken(b, NAMESPACE_SEPARATOR);
    return true;
  }

  // (namespace_definition NAMESPACE_SEPARATOR)*
  private static boolean fqn_container_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "fqn_container_1")) return false;
    while (true) {
      int c = current_position_(b);
      if (!fqn_container_1_0(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "fqn_container_1", c)) break;
    }
    return true;
  }

  // namespace_definition NAMESPACE_SEPARATOR
  private static boolean fqn_container_1_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "fqn_container_1_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = namespace_definition(b, l + 1);
    r = r && consumeToken(b, NAMESPACE_SEPARATOR);
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // regular_name_wrapper !('{'|FARROW|LPAREN|CONSUMES|PRODUCES) [<<checkHasSpaceBefore expressionAssignments>>]
  public static boolean function_call_statement(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "function_call_statement")) return false;
    if (!nextTokenIs(b, NAME)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = regular_name_wrapper(b, l + 1);
    r = r && function_call_statement_1(b, l + 1);
    r = r && function_call_statement_2(b, l + 1);
    exit_section_(b, m, FUNCTION_CALL_STATEMENT, r);
    return r;
  }

  // !('{'|FARROW|LPAREN|CONSUMES|PRODUCES)
  private static boolean function_call_statement_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "function_call_statement_1")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NOT_);
    r = !function_call_statement_1_0(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  // '{'|FARROW|LPAREN|CONSUMES|PRODUCES
  private static boolean function_call_statement_1_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "function_call_statement_1_0")) return false;
    boolean r;
    r = consumeToken(b, LBRACE);
    if (!r) r = consumeToken(b, FARROW);
    if (!r) r = consumeToken(b, LPAREN);
    if (!r) r = consumeToken(b, CONSUMES);
    if (!r) r = consumeToken(b, PRODUCES);
    return r;
  }

  // [<<checkHasSpaceBefore expressionAssignments>>]
  private static boolean function_call_statement_2(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "function_call_statement_2")) return false;
    checkHasSpaceBefore(b, l + 1, PuppetParserGenerated::expressionAssignments);
    return true;
  }

  /* ********************************************************** */
  // '>>' data_type
  public static boolean function_return_type(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "function_return_type")) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_, FUNCTION_RETURN_TYPE, "<function return type>");
    r = consumeToken(b, RSHIFT);
    p = r; // pin = 1
    r = r && data_type(b, l + 1);
    exit_section_(b, l, m, r, p, PuppetParserGenerated::function_return_type_recover);
    return r || p;
  }

  /* ********************************************************** */
  // !(common_recovery_tokens | '{')
  static boolean function_return_type_recover(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "function_return_type_recover")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NOT_);
    r = !function_return_type_recover_0(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  // common_recovery_tokens | '{'
  private static boolean function_return_type_recover_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "function_return_type_recover_0")) return false;
    boolean r;
    r = common_recovery_tokens(b, l + 1);
    if (!r) r = consumeToken(b, LBRACE);
    return r;
  }

  /* ********************************************************** */
  // <<isPuppet3>> varWrapper
  static boolean hashAccessSource(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "hashAccessSource")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = isPuppet3(b, l + 1);
    r = r && varWrapper(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // hashAccessSource hash_array_index
  public static boolean hashArrayAccess(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "hashArrayAccess")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, HASH_ARRAY_ACCESS, "<hash array access>");
    r = hashAccessSource(b, l + 1);
    r = r && hash_array_index(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  /* ********************************************************** */
  // key '=>' expression
  public static boolean hashPair(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "hashPair")) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_, HASH_PAIR, "<hash pair>");
    r = key(b, l + 1);
    p = r; // pin = 1
    r = r && report_error_(b, consumeToken(b, FARROW));
    r = p && expression(b, l + 1, -1) && r;
    exit_section_(b, l, m, r, p, PuppetParserGenerated::recoverHashPair);
    return r || p;
  }

  /* ********************************************************** */
  // hashPair (',' (hashPair | &'}'))*
  static boolean hashPairsList(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "hashPairsList")) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_);
    r = hashPair(b, l + 1);
    p = r; // pin = 1
    r = r && hashPairsList_1(b, l + 1);
    exit_section_(b, l, m, r, p, null);
    return r || p;
  }

  // (',' (hashPair | &'}'))*
  private static boolean hashPairsList_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "hashPairsList_1")) return false;
    while (true) {
      int c = current_position_(b);
      if (!hashPairsList_1_0(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "hashPairsList_1", c)) break;
    }
    return true;
  }

  // ',' (hashPair | &'}')
  private static boolean hashPairsList_1_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "hashPairsList_1_0")) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_);
    r = consumeToken(b, COMMA);
    p = r; // pin = 1
    r = r && hashPairsList_1_0_1(b, l + 1);
    exit_section_(b, l, m, r, p, null);
    return r || p;
  }

  // hashPair | &'}'
  private static boolean hashPairsList_1_0_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "hashPairsList_1_0_1")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = hashPair(b, l + 1);
    if (!r) r = hashPairsList_1_0_1_1(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // &'}'
  private static boolean hashPairsList_1_0_1_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "hashPairsList_1_0_1_1")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _AND_);
    r = consumeToken(b, RBRACE);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  /* ********************************************************** */
  // <<isPuppet4>> '*' '=>' expression
  public static boolean hash_argument(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "hash_argument")) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_, HASH_ARGUMENT, "<hash argument>");
    r = isPuppet4(b, l + 1);
    r = r && consumeTokens(b, 1, TIMES, FARROW);
    p = r; // pin = 2
    r = r && expression(b, l + 1, -1);
    exit_section_(b, l, m, r, p, null);
    return r || p;
  }

  /* ********************************************************** */
  // '[' expression ']'
  public static boolean hash_array_index(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "hash_array_index")) return false;
    if (!nextTokenIs(b, LBRACK)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, LBRACK);
    r = r && expression(b, l + 1, -1);
    r = r && consumeToken(b, RBRACK);
    exit_section_(b, m, HASH_ARRAY_INDEX, r);
    return r;
  }

  /* ********************************************************** */
  // heredoc_body_item+
  static boolean heredoc_body(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "heredoc_body")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = heredoc_body_item(b, l + 1);
    while (r) {
      int c = current_position_(b);
      if (!heredoc_body_item(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "heredoc_body", c)) break;
    }
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // DOUBLE_QUOTED_STRING_MIDDLE | interpolatedPart
  static boolean heredoc_body_item(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "heredoc_body_item")) return false;
    boolean r;
    r = consumeToken(b, DOUBLE_QUOTED_STRING_MIDDLE);
    if (!r) r = interpolatedPart(b, l + 1);
    return r;
  }

  /* ********************************************************** */
  // STRING | SINGLE_QUOTED_STRING | DOUBLE_QUOTED_STRING
  static boolean importString(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "importString")) return false;
    boolean r;
    r = consumeToken(b, STRING);
    if (!r) r = consumeToken(b, SINGLE_QUOTED_STRING);
    if (!r) r = consumeToken(b, DOUBLE_QUOTED_STRING);
    return r;
  }

  /* ********************************************************** */
  // resource_like_class_declaration_prefix resource_like_class_declaration_block
  public static boolean include_class_expression(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "include_class_expression")) return false;
    if (!nextTokenIs(b, CLASS)) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_, INCLUDE_CLASS_EXPRESSION, null);
    r = resource_like_class_declaration_prefix(b, l + 1);
    p = r; // pin = 1
    r = r && resource_like_class_declaration_block(b, l + 1);
    exit_section_(b, l, m, r, p, null);
    return r || p;
  }

  /* ********************************************************** */
  // <<isIncludeClassFunction>> regular_name_wrapper ('(' classNameList ')' | classNameList )
  public static boolean include_class_statement(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "include_class_statement")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, INCLUDE_CLASS_STATEMENT, "<include class statement>");
    r = isIncludeClassFunction(b, l + 1);
    r = r && regular_name_wrapper(b, l + 1);
    r = r && include_class_statement_2(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  // '(' classNameList ')' | classNameList
  private static boolean include_class_statement_2(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "include_class_statement_2")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = include_class_statement_2_0(b, l + 1);
    if (!r) r = classNameList(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // '(' classNameList ')'
  private static boolean include_class_statement_2_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "include_class_statement_2_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, LPAREN);
    r = r && classNameList(b, l + 1);
    r = r && consumeToken(b, RPAREN);
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // resource_name ':' resource_arguments_list?
  static boolean instance_like_declaration_body(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "instance_like_declaration_body")) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_);
    r = resource_name(b, l + 1);
    r = r && consumeToken(b, COLON);
    p = r; // pin = 2
    r = r && instance_like_declaration_body_2(b, l + 1);
    exit_section_(b, l, m, r, p, null);
    return r || p;
  }

  // resource_arguments_list?
  private static boolean instance_like_declaration_body_2(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "instance_like_declaration_body_2")) return false;
    resource_arguments_list(b, l + 1);
    return true;
  }

  /* ********************************************************** */
  // instance_like_declaration_body_fallback_to_attribute | instance_like_declaration_body_fallback_to_name
  static boolean instance_like_declaration_body_fallback(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "instance_like_declaration_body_fallback")) return false;
    boolean r;
    r = instance_like_declaration_body_fallback_to_attribute(b, l + 1);
    if (!r) r = instance_like_declaration_body_fallback_to_name(b, l + 1);
    return r;
  }

  /* ********************************************************** */
  // regular_name_wrapper (':'|'=>')
  static boolean instance_like_declaration_body_fallback_to_attribute(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "instance_like_declaration_body_fallback_to_attribute")) return false;
    if (!nextTokenIs(b, NAME)) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_);
    r = regular_name_wrapper(b, l + 1);
    p = r; // pin = 1
    r = r && instance_like_declaration_body_fallback_to_attribute_1(b, l + 1);
    exit_section_(b, l, m, r, p, null);
    return r || p;
  }

  // ':'|'=>'
  private static boolean instance_like_declaration_body_fallback_to_attribute_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "instance_like_declaration_body_fallback_to_attribute_1")) return false;
    boolean r;
    r = consumeToken(b, COLON);
    if (!r) r = consumeToken(b, FARROW);
    return r;
  }

  /* ********************************************************** */
  // resource_name ':'
  static boolean instance_like_declaration_body_fallback_to_name(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "instance_like_declaration_body_fallback_to_name")) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_);
    r = resource_name(b, l + 1);
    p = r; // pin = 1
    r = r && consumeToken(b, COLON);
    exit_section_(b, l, m, r, p, null);
    return r || p;
  }

  /* ********************************************************** */
  // instance_like_declaration_body | instance_like_declaration_body_fallback
  static boolean instance_like_declaration_body_with_fallback(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "instance_like_declaration_body_with_fallback")) return false;
    boolean r;
    r = instance_like_declaration_body(b, l + 1);
    if (!r) r = instance_like_declaration_body_fallback(b, l + 1);
    return r;
  }

  /* ********************************************************** */
  // varWrapper | interpolated_block
  static boolean interpolatedPart(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "interpolatedPart")) return false;
    if (!nextTokenIs(b, "", DOLLAR, VAR_INTERPOLATION_START)) return false;
    boolean r;
    r = varWrapper(b, l + 1);
    if (!r) r = interpolated_block(b, l + 1);
    return r;
  }

  /* ********************************************************** */
  // VAR_INTERPOLATION_START expression VAR_INTERPOLATION_END
  public static boolean interpolated_block(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "interpolated_block")) return false;
    if (!nextTokenIs(b, VAR_INTERPOLATION_START)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, VAR_INTERPOLATION_START);
    r = r && expression(b, l + 1, -1);
    r = r && consumeToken(b, VAR_INTERPOLATION_END);
    exit_section_(b, m, INTERPOLATED_BLOCK, r);
    return r;
  }

  /* ********************************************************** */
  // string_literal | data_type
  static boolean key(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "key")) return false;
    boolean r;
    r = expression(b, l + 1, 11);
    if (!r) r = data_type(b, l + 1);
    return r;
  }

  /* ********************************************************** */
  // <<isKeyword>>
  static boolean keyword(PsiBuilder b, int l) {
    return isKeyword(b, l + 1);
  }

  /* ********************************************************** */
  // NAME &NAMESPACE_SEPARATOR
  public static boolean namespace_definition(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "namespace_definition")) return false;
    if (!nextTokenIs(b, NAME)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, NAME);
    r = r && namespace_definition_1(b, l + 1);
    exit_section_(b, m, NAMESPACE_DEFINITION, r);
    return r;
  }

  // &NAMESPACE_SEPARATOR
  private static boolean namespace_definition_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "namespace_definition_1")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _AND_);
    r = consumeToken(b, NAMESPACE_SEPARATOR);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  /* ********************************************************** */
  // regular_name_wrapper | string_literal | default_wrapper | regexp
  static boolean node_name(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "node_name")) return false;
    boolean r;
    r = regular_name_wrapper(b, l + 1);
    if (!r) r = expression(b, l + 1, 11);
    if (!r) r = default_wrapper(b, l + 1);
    if (!r) r = regexp(b, l + 1);
    return r;
  }

  /* ********************************************************** */
  // node_name (',' node_name)*
  public static boolean node_names_list(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "node_names_list")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, NODE_NAMES_LIST, "<node names list>");
    r = node_name(b, l + 1);
    r = r && node_names_list_1(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  // (',' node_name)*
  private static boolean node_names_list_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "node_names_list_1")) return false;
    while (true) {
      int c = current_position_(b);
      if (!node_names_list_1_0(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "node_names_list_1", c)) break;
    }
    return true;
  }

  // ',' node_name
  private static boolean node_names_list_1_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "node_names_list_1_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, COMMA);
    r = r && node_name(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // !(';' | DIV | common_recovery_tokens)
  static boolean paramListRecover(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "paramListRecover")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NOT_);
    r = !paramListRecover_0(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  // ';' | DIV | common_recovery_tokens
  private static boolean paramListRecover_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "paramListRecover_0")) return false;
    boolean r;
    r = consumeToken(b, SEMIC);
    if (!r) r = consumeToken(b, DIV);
    if (!r) r = common_recovery_tokens(b, l + 1);
    return r;
  }

  /* ********************************************************** */
  // !(',' | '}' | ';' | <<isPuppet4>> '*' |  DIV  | common_recovery_tokens)
  static boolean paramRecoverWhile(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "paramRecoverWhile")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NOT_);
    r = !paramRecoverWhile_0(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  // ',' | '}' | ';' | <<isPuppet4>> '*' |  DIV  | common_recovery_tokens
  private static boolean paramRecoverWhile_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "paramRecoverWhile_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, COMMA);
    if (!r) r = consumeToken(b, RBRACE);
    if (!r) r = consumeToken(b, SEMIC);
    if (!r) r = paramRecoverWhile_0_3(b, l + 1);
    if (!r) r = consumeToken(b, DIV);
    if (!r) r = common_recovery_tokens(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // <<isPuppet4>> '*'
  private static boolean paramRecoverWhile_0_3(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "paramRecoverWhile_0_3")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = isPuppet4(b, l + 1);
    r = r && consumeToken(b, TIMES);
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // {varWrapper | typed_parameter } ['=' expression]
  public static boolean parameter(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "parameter")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, PARAMETER, "<parameter>");
    r = parameter_0(b, l + 1);
    r = r && parameter_1(b, l + 1);
    exit_section_(b, l, m, r, false, PuppetParserGenerated::recover_parameter);
    return r;
  }

  // varWrapper | typed_parameter
  private static boolean parameter_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "parameter_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = varWrapper(b, l + 1);
    if (!r) r = typed_parameter(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // ['=' expression]
  private static boolean parameter_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "parameter_1")) return false;
    parameter_1_0(b, l + 1);
    return true;
  }

  // '=' expression
  private static boolean parameter_1_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "parameter_1_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, EQUALS);
    r = r && expression(b, l + 1, -1);
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // parameter (',' (parameter | &')'))*
  static boolean parameters_list(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "parameters_list")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = parameter(b, l + 1);
    r = r && parameters_list_1(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // (',' (parameter | &')'))*
  private static boolean parameters_list_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "parameters_list_1")) return false;
    while (true) {
      int c = current_position_(b);
      if (!parameters_list_1_0(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "parameters_list_1", c)) break;
    }
    return true;
  }

  // ',' (parameter | &')')
  private static boolean parameters_list_1_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "parameters_list_1_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, COMMA);
    r = r && parameters_list_1_0_1(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // parameter | &')'
  private static boolean parameters_list_1_0_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "parameters_list_1_0_1")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = parameter(b, l + 1);
    if (!r) r = parameters_list_1_0_1_1(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // &')'
  private static boolean parameters_list_1_0_1_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "parameters_list_1_0_1_1")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _AND_);
    r = consumeToken(b, RPAREN);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  /* ********************************************************** */
  // <<isPuppet3>> INHERITS node_name
  public static boolean parent_node(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "parent_node")) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_, PARENT_NODE, "<parent node>");
    r = isPuppet3(b, l + 1);
    r = r && consumeToken(b, INHERITS);
    p = r; // pin = 2
    r = r && node_name(b, l + 1);
    exit_section_(b, l, m, r, p, null);
    return r || p;
  }

  /* ********************************************************** */
  // '(' <<parenthesized_element_content <<x1>>>> ')'
  static boolean parenthesized_element(PsiBuilder b, int l, Parser _x1) {
    if (!recursion_guard_(b, l, "parenthesized_element")) return false;
    if (!nextTokenIs(b, LPAREN)) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_);
    r = consumeToken(b, LPAREN);
    p = r; // pin = 1
    r = r && report_error_(b, parenthesized_element_content(b, l + 1, _x1));
    r = p && consumeToken(b, RPAREN) && r;
    exit_section_(b, l, m, r, p, null);
    return r || p;
  }

  /* ********************************************************** */
  // <<x1>>
  static boolean parenthesized_element_content(PsiBuilder b, int l, Parser _x1) {
    if (!recursion_guard_(b, l, "parenthesized_element_content")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_);
    r = _x1.parse(b, l);
    exit_section_(b, l, m, r, false, PuppetParserGenerated::recoverParenthesized);
    return r;
  }

  /* ********************************************************** */
  // <<parenthesized_element expressionAssignments?>>
  public static boolean parenthesized_expressions_list_block(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "parenthesized_expressions_list_block")) return false;
    if (!nextTokenIs(b, LPAREN)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = parenthesized_element(b, l + 1, PuppetParserGenerated::parenthesized_expressions_list_block_0_0);
    exit_section_(b, m, PARENTHESIZED_EXPRESSIONS_LIST_BLOCK, r);
    return r;
  }

  // expressionAssignments?
  private static boolean parenthesized_expressions_list_block_0_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "parenthesized_expressions_list_block_0_0")) return false;
    expressionAssignments(b, l + 1);
    return true;
  }

  /* ********************************************************** */
  // <<parenthesized_element parameters_list?>>
  public static boolean parenthesized_parameters_list_block(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "parenthesized_parameters_list_block")) return false;
    if (!nextTokenIs(b, LPAREN)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = parenthesized_element(b, l + 1, PuppetParserGenerated::parenthesized_parameters_list_block_0_0);
    exit_section_(b, m, PARENTHESIZED_PARAMETERS_LIST_BLOCK, r);
    return r;
  }

  // parameters_list?
  private static boolean parenthesized_parameters_list_block_0_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "parenthesized_parameters_list_block_0_0")) return false;
    parameters_list(b, l + 1);
    return true;
  }

  /* ********************************************************** */
  // '|' <<piped_element_content <<x1>>>> '|'
  static boolean piped_element(PsiBuilder b, int l, Parser _x1) {
    if (!recursion_guard_(b, l, "piped_element")) return false;
    if (!nextTokenIs(b, PIPE)) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_);
    r = consumeToken(b, PIPE);
    p = r; // pin = 1
    r = r && report_error_(b, piped_element_content(b, l + 1, _x1));
    r = p && consumeToken(b, PIPE) && r;
    exit_section_(b, l, m, r, p, null);
    return r || p;
  }

  /* ********************************************************** */
  // <<x1>>
  static boolean piped_element_content(PsiBuilder b, int l, Parser _x1) {
    if (!recursion_guard_(b, l, "piped_element_content")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_);
    r = _x1.parse(b, l);
    exit_section_(b, l, m, r, false, PuppetParserGenerated::recoverPiped);
    return r;
  }

  /* ********************************************************** */
  // <<piped_element parameters_list?>>
  public static boolean piped_parameters_list_block(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "piped_parameters_list_block")) return false;
    if (!nextTokenIs(b, PIPE)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = piped_element(b, l + 1, PuppetParserGenerated::piped_parameters_list_block_0_0);
    exit_section_(b, m, PIPED_PARAMETERS_LIST_BLOCK, r);
    return r;
  }

  // parameters_list?
  private static boolean piped_parameters_list_block_0_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "piped_parameters_list_block_0_0")) return false;
    parameters_list(b, l + 1);
    return true;
  }

  /* ********************************************************** */
  // <<isPuppet4>> LISTSTART <<bracketed_element_content expressions_list_with_tailing_comma?>> ']'
  static boolean puppet4_array(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "puppet4_array")) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_);
    r = isPuppet4(b, l + 1);
    r = r && consumeToken(b, LISTSTART);
    p = r; // pin = 2
    r = r && report_error_(b, bracketed_element_content(b, l + 1, PuppetParserGenerated::puppet4_array_2_0));
    r = p && consumeToken(b, RBRACK) && r;
    exit_section_(b, l, m, r, p, null);
    return r || p;
  }

  // expressions_list_with_tailing_comma?
  private static boolean puppet4_array_2_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "puppet4_array_2_0")) return false;
    expressions_list_with_tailing_comma(b, l + 1);
    return true;
  }

  /* ********************************************************** */
  // <<parseFileContents puppet_file_contents>>
  static boolean puppetFile(PsiBuilder b, int l) {
    return parseFileContents(b, l + 1, PuppetParserGenerated::puppet_file_contents);
  }

  /* ********************************************************** */
  // (statements | <<consumeOneToken>>)*
  static boolean puppet_file_contents(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "puppet_file_contents")) return false;
    while (true) {
      int c = current_position_(b);
      if (!puppet_file_contents_0(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "puppet_file_contents", c)) break;
    }
    return true;
  }

  // statements | <<consumeOneToken>>
  private static boolean puppet_file_contents_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "puppet_file_contents_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = statements(b, l + 1);
    if (!r) r = consumeOneToken(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // !'}'
  static boolean recoverBraced(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "recoverBraced")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NOT_);
    r = !consumeToken(b, RBRACE);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  /* ********************************************************** */
  // !']'
  static boolean recoverBracketed(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "recoverBracketed")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NOT_);
    r = !consumeToken(b, RBRACK);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  /* ********************************************************** */
  // !(','|'}')
  static boolean recoverHashPair(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "recoverHashPair")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NOT_);
    r = !recoverHashPair_0(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  // ','|'}'
  private static boolean recoverHashPair_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "recoverHashPair_0")) return false;
    boolean r;
    r = consumeToken(b, COMMA);
    if (!r) r = consumeToken(b, RBRACE);
    return r;
  }

  /* ********************************************************** */
  // !')'
  static boolean recoverParenthesized(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "recoverParenthesized")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NOT_);
    r = !consumeToken(b, RPAREN);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  /* ********************************************************** */
  // !'|'
  static boolean recoverPiped(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "recoverPiped")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NOT_);
    r = !consumeToken(b, PIPE);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  /* ********************************************************** */
  // !(','|'}')
  static boolean recover_braced_selector_value(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "recover_braced_selector_value")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NOT_);
    r = !recover_braced_selector_value_0(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  // ','|'}'
  private static boolean recover_braced_selector_value_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "recover_braced_selector_value_0")) return false;
    boolean r;
    r = consumeToken(b, COMMA);
    if (!r) r = consumeToken(b, RBRACE);
    return r;
  }

  /* ********************************************************** */
  // !( '!' | '(' | '-' | '[' | '*' | LISTSTART | '{' | CAPITALIZED_NAME | CASE | DEFAULT | DOLLAR | DOUBLE_QUOTED_STRING | DOUBLE_QUOTED_STRING_START
  //   | FALSE | FLOAT_LITERAL | HEREDOC_AT | INTEGER_LITERAL_WITHOUTQ | NAME | REGEX | SINGLE_QUOTED_STRING | TRUE | UNDEF | '}')
  static boolean recover_case_option(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "recover_case_option")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NOT_);
    r = !recover_case_option_0(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  // '!' | '(' | '-' | '[' | '*' | LISTSTART | '{' | CAPITALIZED_NAME | CASE | DEFAULT | DOLLAR | DOUBLE_QUOTED_STRING | DOUBLE_QUOTED_STRING_START
  //   | FALSE | FLOAT_LITERAL | HEREDOC_AT | INTEGER_LITERAL_WITHOUTQ | NAME | REGEX | SINGLE_QUOTED_STRING | TRUE | UNDEF | '}'
  private static boolean recover_case_option_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "recover_case_option_0")) return false;
    boolean r;
    r = consumeToken(b, NOT);
    if (!r) r = consumeToken(b, LPAREN);
    if (!r) r = consumeToken(b, MINUS);
    if (!r) r = consumeToken(b, LBRACK);
    if (!r) r = consumeToken(b, TIMES);
    if (!r) r = consumeToken(b, LISTSTART);
    if (!r) r = consumeToken(b, LBRACE);
    if (!r) r = consumeToken(b, CAPITALIZED_NAME);
    if (!r) r = consumeToken(b, CASE);
    if (!r) r = consumeToken(b, DEFAULT);
    if (!r) r = consumeToken(b, DOLLAR);
    if (!r) r = consumeToken(b, DOUBLE_QUOTED_STRING);
    if (!r) r = consumeToken(b, DOUBLE_QUOTED_STRING_START);
    if (!r) r = consumeToken(b, FALSE);
    if (!r) r = consumeToken(b, FLOAT_LITERAL);
    if (!r) r = consumeToken(b, HEREDOC_AT);
    if (!r) r = consumeToken(b, INTEGER_LITERAL_WITHOUTQ);
    if (!r) r = consumeToken(b, NAME);
    if (!r) r = consumeToken(b, REGEX);
    if (!r) r = consumeToken(b, SINGLE_QUOTED_STRING);
    if (!r) r = consumeToken(b, TRUE);
    if (!r) r = consumeToken(b, UNDEF);
    if (!r) r = consumeToken(b, RBRACE);
    return r;
  }

  /* ********************************************************** */
  // <<recoverParameter recover_parameter_inner>>
  static boolean recover_parameter(PsiBuilder b, int l) {
    return recoverParameter(b, l + 1, PuppetParserGenerated::recover_parameter_inner);
  }

  /* ********************************************************** */
  // !(')'|','|'|')
  static boolean recover_parameter_inner(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "recover_parameter_inner")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NOT_);
    r = !recover_parameter_inner_0(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  // ')'|','|'|'
  private static boolean recover_parameter_inner_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "recover_parameter_inner_0")) return false;
    boolean r;
    r = consumeToken(b, RPAREN);
    if (!r) r = consumeToken(b, COMMA);
    if (!r) r = consumeToken(b, PIPE);
    return r;
  }

  /* ********************************************************** */
  // <<recoverTypedParameter recover_typed_parameter_inner>>
  static boolean recover_typed_parameter(PsiBuilder b, int l) {
    return recoverTypedParameter(b, l + 1, PuppetParserGenerated::recover_typed_parameter_inner);
  }

  /* ********************************************************** */
  // !(')'|','|'|'|'=')
  static boolean recover_typed_parameter_inner(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "recover_typed_parameter_inner")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NOT_);
    r = !recover_typed_parameter_inner_0(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  // ')'|','|'|'|'='
  private static boolean recover_typed_parameter_inner_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "recover_typed_parameter_inner_0")) return false;
    boolean r;
    r = consumeToken(b, RPAREN);
    if (!r) r = consumeToken(b, COMMA);
    if (!r) r = consumeToken(b, PIPE);
    if (!r) r = consumeToken(b, EQUALS);
    return r;
  }

  /* ********************************************************** */
  // '=~' | '!~'
  static boolean regexOp(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "regexOp")) return false;
    if (!nextTokenIs(b, "", MATCH, NOMATCH)) return false;
    boolean r;
    r = consumeToken(b, MATCH);
    if (!r) r = consumeToken(b, NOMATCH);
    return r;
  }

  /* ********************************************************** */
  // resource_default_statement |resource_declaration | include_class_expression |  expression
  static boolean resource(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "resource")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_);
    r = resource_default_statement(b, l + 1);
    if (!r) r = resource_declaration(b, l + 1);
    if (!r) r = include_class_expression(b, l + 1);
    if (!r) r = expression(b, l + 1, -1);
    exit_section_(b, l, m, r, false, PuppetParserGenerated::resource_recover_while);
    return r;
  }

  /* ********************************************************** */
  // !(';' | DIV | common_recovery_tokens)
  static boolean resourceInstanceRecover(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "resourceInstanceRecover")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NOT_);
    r = !resourceInstanceRecover_0(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  // ';' | DIV | common_recovery_tokens
  private static boolean resourceInstanceRecover_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "resourceInstanceRecover_0")) return false;
    boolean r;
    r = consumeToken(b, SEMIC);
    if (!r) r = consumeToken(b, DIV);
    if (!r) r = common_recovery_tokens(b, l + 1);
    return r;
  }

  /* ********************************************************** */
  // arguments_list
  public static boolean resource_arguments_list(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "resource_arguments_list")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, RESOURCE_ARGUMENTS_LIST, "<resource arguments list>");
    r = arguments_list(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  /* ********************************************************** */
  // ['@' | '@@'] resource_type braced_resource_by_classname_contents_block
  public static boolean resource_declaration(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "resource_declaration")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _COLLAPSE_, RESOURCE_DECLARATION, "<resource declaration>");
    r = resource_declaration_0(b, l + 1);
    r = r && resource_type(b, l + 1);
    r = r && braced_resource_by_classname_contents_block(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  // ['@' | '@@']
  private static boolean resource_declaration_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "resource_declaration_0")) return false;
    resource_declaration_0_0(b, l + 1);
    return true;
  }

  // '@' | '@@'
  private static boolean resource_declaration_0_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "resource_declaration_0_0")) return false;
    boolean r;
    r = consumeToken(b, AT);
    if (!r) r = consumeToken(b, ATAT);
    return r;
  }

  /* ********************************************************** */
  // data_type &('{' NAME {'=>'|'+>'}) braced_any_arguments_list_block
  public static boolean resource_default_statement(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "resource_default_statement")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, RESOURCE_DEFAULT_STATEMENT, "<resource default statement>");
    r = data_type(b, l + 1);
    r = r && resource_default_statement_1(b, l + 1);
    r = r && braced_any_arguments_list_block(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  // &('{' NAME {'=>'|'+>'})
  private static boolean resource_default_statement_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "resource_default_statement_1")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _AND_);
    r = resource_default_statement_1_0(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  // '{' NAME {'=>'|'+>'}
  private static boolean resource_default_statement_1_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "resource_default_statement_1_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeTokens(b, 0, LBRACE, NAME);
    r = r && resource_default_statement_1_0_2(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // '=>'|'+>'
  private static boolean resource_default_statement_1_0_2(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "resource_default_statement_1_0_2")) return false;
    boolean r;
    r = consumeToken(b, FARROW);
    if (!r) r = consumeToken(b, PARROW);
    return r;
  }

  /* ********************************************************** */
  // instance_like_declaration_body_with_fallback
  public static boolean resource_instance_declaration(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "resource_instance_declaration")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, RESOURCE_INSTANCE_DECLARATION, "<resource instance declaration>");
    r = instance_like_declaration_body_with_fallback(b, l + 1);
    exit_section_(b, l, m, r, false, PuppetParserGenerated::resourceInstanceRecover);
    return r;
  }

  /* ********************************************************** */
  // <<braced_element <<resource_like_declarations_list resource_like_class_description>>>>
  public static boolean resource_like_class_declaration_block(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "resource_like_class_declaration_block")) return false;
    if (!nextTokenIs(b, LBRACE)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = braced_element(b, l + 1, resource_like_class_declaration_block_0_0_parser_);
    exit_section_(b, m, RESOURCE_LIKE_CLASS_DECLARATION_BLOCK, r);
    return r;
  }

  /* ********************************************************** */
  // CLASS &'{'
  static boolean resource_like_class_declaration_prefix(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "resource_like_class_declaration_prefix")) return false;
    if (!nextTokenIs(b, CLASS)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, CLASS);
    r = r && resource_like_class_declaration_prefix_1(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // &'{'
  private static boolean resource_like_class_declaration_prefix_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "resource_like_class_declaration_prefix_1")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _AND_);
    r = consumeToken(b, LBRACE);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  /* ********************************************************** */
  // !DEFAULT instance_like_declaration_body_with_fallback
  public static boolean resource_like_class_description(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "resource_like_class_description")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, RESOURCE_LIKE_CLASS_DESCRIPTION, "<resource like class description>");
    r = resource_like_class_description_0(b, l + 1);
    r = r && instance_like_declaration_body_with_fallback(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  // !DEFAULT
  private static boolean resource_like_class_description_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "resource_like_class_description_0")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NOT_);
    r = !consumeToken(b, DEFAULT);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  /* ********************************************************** */
  static Parser resource_like_declarations_list_$(Parser _x1) {
    return (b, l) -> resource_like_declarations_list(b, l + 1, _x1);
  }

  // [<<x1>> (';' (<<x1>>| &'}'))*]
  static boolean resource_like_declarations_list(PsiBuilder b, int l, Parser _x1) {
    if (!recursion_guard_(b, l, "resource_like_declarations_list")) return false;
    resource_like_declarations_list_0(b, l + 1, _x1);
    return true;
  }

  // <<x1>> (';' (<<x1>>| &'}'))*
  private static boolean resource_like_declarations_list_0(PsiBuilder b, int l, Parser _x1) {
    if (!recursion_guard_(b, l, "resource_like_declarations_list_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = _x1.parse(b, l);
    r = r && resource_like_declarations_list_0_1(b, l + 1, _x1);
    exit_section_(b, m, null, r);
    return r;
  }

  // (';' (<<x1>>| &'}'))*
  private static boolean resource_like_declarations_list_0_1(PsiBuilder b, int l, Parser _x1) {
    if (!recursion_guard_(b, l, "resource_like_declarations_list_0_1")) return false;
    while (true) {
      int c = current_position_(b);
      if (!resource_like_declarations_list_0_1_0(b, l + 1, _x1)) break;
      if (!empty_element_parsed_guard_(b, "resource_like_declarations_list_0_1", c)) break;
    }
    return true;
  }

  // ';' (<<x1>>| &'}')
  private static boolean resource_like_declarations_list_0_1_0(PsiBuilder b, int l, Parser _x1) {
    if (!recursion_guard_(b, l, "resource_like_declarations_list_0_1_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, SEMIC);
    r = r && resource_like_declarations_list_0_1_0_1(b, l + 1, _x1);
    exit_section_(b, m, null, r);
    return r;
  }

  // <<x1>>| &'}'
  private static boolean resource_like_declarations_list_0_1_0_1(PsiBuilder b, int l, Parser _x1) {
    if (!recursion_guard_(b, l, "resource_like_declarations_list_0_1_0_1")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = _x1.parse(b, l);
    if (!r) r = resource_like_declarations_list_0_1_0_1_1(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // &'}'
  private static boolean resource_like_declarations_list_0_1_0_1_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "resource_like_declarations_list_0_1_0_1_1")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _AND_);
    r = consumeToken(b, RBRACE);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  /* ********************************************************** */
  // selector
  //   | array
  //   | hashArrayAccesses
  //   | quoted_text
  //   | varWrapper
  //   | default_wrapper
  //   | regular_name_wrapper
  static boolean resource_name(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "resource_name")) return false;
    boolean r;
    r = expression(b, l + 1, 6);
    if (!r) r = array(b, l + 1);
    if (!r) r = hashArrayAccesses(b, l + 1);
    if (!r) r = quoted_text(b, l + 1);
    if (!r) r = varWrapper(b, l + 1);
    if (!r) r = default_wrapper(b, l + 1);
    if (!r) r = regular_name_wrapper(b, l + 1);
    return r;
  }

  /* ********************************************************** */
  // !(resource_recovery_tokens)
  static boolean resource_recover_while(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "resource_recover_while")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NOT_);
    r = !resource_recover_while_0(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  // (resource_recovery_tokens)
  private static boolean resource_recover_while_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "resource_recover_while_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = resource_recovery_tokens(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // '<-' | '->' | '<~' | '~>' | expression_relation_recovery_tokens
  static boolean resource_recovery_tokens(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "resource_recovery_tokens")) return false;
    boolean r;
    r = consumeToken(b, OUT_EDGE);
    if (!r) r = consumeToken(b, IN_EDGE);
    if (!r) r = consumeToken(b, OUT_EDGE_SUB);
    if (!r) r = consumeToken(b, IN_EDGE_SUB);
    if (!r) r = expression_relation_recovery_tokens(b, l + 1);
    return r;
  }

  /* ********************************************************** */
  // regular_name_wrapper | data_type
  static boolean resource_type(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "resource_type")) return false;
    boolean r;
    r = regular_name_wrapper(b, l + 1);
    if (!r) r = data_type(b, l + 1);
    return r;
  }

  /* ********************************************************** */
  // braced_selector_values_block | selector_value
  static boolean selector_content(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "selector_content")) return false;
    boolean r;
    r = braced_selector_values_block(b, l + 1);
    if (!r) r = selector_value(b, l + 1);
    return r;
  }

  /* ********************************************************** */
  // expression '=>' expression
  public static boolean selector_value(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "selector_value")) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_, SELECTOR_VALUE, "<selector value>");
    r = expression(b, l + 1, -1);
    p = r; // pin = 1
    r = r && report_error_(b, consumeToken(b, FARROW));
    r = p && expression(b, l + 1, -1) && r;
    exit_section_(b, l, m, r, p, null);
    return r || p;
  }

  /* ********************************************************** */
  // braced_selector_value_element (',' (braced_selector_value_element | &'}'))*
  static boolean selector_values(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "selector_values")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = braced_selector_value_element(b, l + 1);
    r = r && selector_values_1(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // (',' (braced_selector_value_element | &'}'))*
  private static boolean selector_values_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "selector_values_1")) return false;
    while (true) {
      int c = current_position_(b);
      if (!selector_values_1_0(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "selector_values_1", c)) break;
    }
    return true;
  }

  // ',' (braced_selector_value_element | &'}')
  private static boolean selector_values_1_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "selector_values_1_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, COMMA);
    r = r && selector_values_1_0_1(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // braced_selector_value_element | &'}'
  private static boolean selector_values_1_0_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "selector_values_1_0_1")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = braced_selector_value_element(b, l + 1);
    if (!r) r = selector_values_1_0_1_1(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // &'}'
  private static boolean selector_values_1_0_1_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "selector_values_1_0_1_1")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _AND_);
    r = consumeToken(b, RBRACE);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  /* ********************************************************** */
  // argument | hash_argument
  static boolean set_argument(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "set_argument")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_);
    r = argument(b, l + 1);
    if (!r) r = hash_argument(b, l + 1);
    exit_section_(b, l, m, r, false, PuppetParserGenerated::paramRecoverWhile);
    return r;
  }

  /* ********************************************************** */
  // statement_call  | expressionAssignment
  static boolean statement(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "statement")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_);
    r = statement_call(b, l + 1);
    if (!r) r = expressionAssignment(b, l + 1);
    exit_section_(b, l, m, r, false, PuppetParserGenerated::expression_assignment_recover_while);
    return r;
  }

  /* ********************************************************** */
  // include_class_statement | function_call_statement
  static boolean statement_call(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "statement_call")) return false;
    boolean r;
    r = include_class_statement(b, l + 1);
    if (!r) r = function_call_statement(b, l + 1);
    return r;
  }

  /* ********************************************************** */
  // statement (';'? statement)*
  static boolean statements(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "statements")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = statement(b, l + 1);
    r = r && statements_1(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // (';'? statement)*
  private static boolean statements_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "statements_1")) return false;
    while (true) {
      int c = current_position_(b);
      if (!statements_1_0(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "statements_1", c)) break;
    }
    return true;
  }

  // ';'? statement
  private static boolean statements_1_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "statements_1_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = statements_1_0_0(b, l + 1);
    r = r && statement(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // ';'?
  private static boolean statements_1_0_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "statements_1_0_0")) return false;
    consumeToken(b, SEMIC);
    return true;
  }

  /* ********************************************************** */
  // DOUBLE_QUOTED_STRING_START stringTemplateInt DOUBLE_QUOTED_STRING_END
  static boolean stringTemplate(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "stringTemplate")) return false;
    if (!nextTokenIs(b, DOUBLE_QUOTED_STRING_START)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, DOUBLE_QUOTED_STRING_START);
    r = r && stringTemplateInt(b, l + 1);
    r = r && consumeToken(b, DOUBLE_QUOTED_STRING_END);
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // interpolatedPart (DOUBLE_QUOTED_STRING_MIDDLE? interpolatedPart)*
  static boolean stringTemplateInt(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "stringTemplateInt")) return false;
    if (!nextTokenIs(b, "", DOLLAR, VAR_INTERPOLATION_START)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = interpolatedPart(b, l + 1);
    r = r && stringTemplateInt_1(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // (DOUBLE_QUOTED_STRING_MIDDLE? interpolatedPart)*
  private static boolean stringTemplateInt_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "stringTemplateInt_1")) return false;
    while (true) {
      int c = current_position_(b);
      if (!stringTemplateInt_1_0(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "stringTemplateInt_1", c)) break;
    }
    return true;
  }

  // DOUBLE_QUOTED_STRING_MIDDLE? interpolatedPart
  private static boolean stringTemplateInt_1_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "stringTemplateInt_1_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = stringTemplateInt_1_0_0(b, l + 1);
    r = r && interpolatedPart(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // DOUBLE_QUOTED_STRING_MIDDLE?
  private static boolean stringTemplateInt_1_0_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "stringTemplateInt_1_0_0")) return false;
    consumeToken(b, DOUBLE_QUOTED_STRING_MIDDLE);
    return true;
  }

  /* ********************************************************** */
  // importString (',' importString)*
  static boolean stringsList(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "stringsList")) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_);
    r = importString(b, l + 1);
    p = r; // pin = 1
    r = r && stringsList_1(b, l + 1);
    exit_section_(b, l, m, r, p, null);
    return r || p;
  }

  // (',' importString)*
  private static boolean stringsList_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "stringsList_1")) return false;
    while (true) {
      int c = current_position_(b);
      if (!stringsList_1_0(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "stringsList_1", c)) break;
    }
    return true;
  }

  // ',' importString
  private static boolean stringsList_1_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "stringsList_1_0")) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_);
    r = consumeToken(b, COMMA);
    p = r; // pin = 1
    r = r && importString(b, l + 1);
    exit_section_(b, l, m, r, p, null);
    return r || p;
  }

  /* ********************************************************** */
  // ';' | '(' | '-' | '{' | ')' | '[' | LISTSTART| NOT | REGEX | common_recovery_tokens
  static boolean top_level_recovery_tokens(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "top_level_recovery_tokens")) return false;
    boolean r;
    r = consumeToken(b, SEMIC);
    if (!r) r = consumeToken(b, LPAREN);
    if (!r) r = consumeToken(b, MINUS);
    if (!r) r = consumeToken(b, LBRACE);
    if (!r) r = consumeToken(b, RPAREN);
    if (!r) r = consumeToken(b, LBRACK);
    if (!r) r = consumeToken(b, LISTSTART);
    if (!r) r = consumeToken(b, NOT);
    if (!r) r = consumeToken(b, REGEX);
    if (!r) r = common_recovery_tokens(b, l + 1);
    return r;
  }

  /* ********************************************************** */
  // <<isPuppet4>> data_type varWrapper
  static boolean typed_parameter(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "typed_parameter")) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_);
    r = isPuppet4(b, l + 1);
    r = r && data_type(b, l + 1);
    p = r; // pin = 2
    r = r && varWrapper(b, l + 1);
    exit_section_(b, l, m, r, p, PuppetParserGenerated::recover_typed_parameter);
    return r || p;
  }

  /* ********************************************************** */
  // Expression root: expression
  // Operator priority table:
  // 0: BINARY(expressionComp)
  // 1: BINARY(expressionIn)
  // 2: BINARY(expressionAdditive)
  // 3: BINARY(expressionMultiplicative)
  // 4: BINARY(expressionBinary)
  // 5: POSTFIX(expressionRegex)
  // 6: PREFIX(expressionUnary)
  // 7: POSTFIX(selector)
  // 8: POSTFIX(expressionCall)
  // 9: ATOM(hashValue)
  // 10: POSTFIX(expressionIndexed)
  // 11: ATOM(expressionParen)
  // 12: POSTFIX(collection) ATOM(hashArrayAccesses) ATOM(array) ATOM(produces_statement)
  //    ATOM(consumes_statement) ATOM(data_type) ATOM(expressionTrue) ATOM(expressionFalse)
  //    ATOM(varWrapper) POSTFIX(function_call_expression) ATOM(quoted_text) ATOM(expressionNumeric)
  //    ATOM(heredocExpression) ATOM(regexp) ATOM(expressionUndef) ATOM(default_wrapper)
  //    ATOM(case_expression) ATOM(if_statement) ATOM(unless_statement) ATOM(import_statement)
  //    ATOM(site_compound) ATOM(type_definition) ATOM(class_definition) ATOM(node_definition)
  //    ATOM(function_definition) ATOM(regular_name_wrapper)
  public static boolean expression(PsiBuilder b, int l, int g) {
    if (!recursion_guard_(b, l, "expression")) return false;
    addVariant(b, "<expression>");
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_, "<expression>");
    r = expressionUnary(b, l + 1);
    if (!r) r = hashValue(b, l + 1);
    if (!r) r = expressionParen(b, l + 1);
    if (!r) r = hashArrayAccesses(b, l + 1);
    if (!r) r = array(b, l + 1);
    if (!r) r = produces_statement(b, l + 1);
    if (!r) r = consumes_statement(b, l + 1);
    if (!r) r = data_type(b, l + 1);
    if (!r) r = expressionTrue(b, l + 1);
    if (!r) r = expressionFalse(b, l + 1);
    if (!r) r = varWrapper(b, l + 1);
    if (!r) r = quoted_text(b, l + 1);
    if (!r) r = expressionNumeric(b, l + 1);
    if (!r) r = heredocExpression(b, l + 1);
    if (!r) r = regexp(b, l + 1);
    if (!r) r = expressionUndef(b, l + 1);
    if (!r) r = default_wrapper(b, l + 1);
    if (!r) r = case_expression(b, l + 1);
    if (!r) r = if_statement(b, l + 1);
    if (!r) r = unless_statement(b, l + 1);
    if (!r) r = import_statement(b, l + 1);
    if (!r) r = site_compound(b, l + 1);
    if (!r) r = type_definition(b, l + 1);
    if (!r) r = class_definition(b, l + 1);
    if (!r) r = node_definition(b, l + 1);
    if (!r) r = function_definition(b, l + 1);
    if (!r) r = regular_name_wrapper(b, l + 1);
    p = r;
    r = r && expression_0(b, l + 1, g);
    exit_section_(b, l, m, null, r, p, null);
    return r || p;
  }

  public static boolean expression_0(PsiBuilder b, int l, int g) {
    if (!recursion_guard_(b, l, "expression_0")) return false;
    boolean r = true;
    while (true) {
      Marker m = enter_section_(b, l, _LEFT_, null);
      if (g < 0 && binaryOp0(b, l + 1)) {
        r = expression(b, l, 0);
        exit_section_(b, l, m, EXPRESSION_COMP, r, true, null);
      }
      else if (g < 1 && binaryOp1(b, l + 1)) {
        r = expression(b, l, 1);
        exit_section_(b, l, m, EXPRESSION_IN, r, true, null);
      }
      else if (g < 2 && binaryOp2(b, l + 1)) {
        r = expression(b, l, 2);
        exit_section_(b, l, m, EXPRESSION_ADDITIVE, r, true, null);
      }
      else if (g < 3 && binaryOp3(b, l + 1)) {
        r = expression(b, l, 3);
        exit_section_(b, l, m, EXPRESSION_MULTIPLICATIVE, r, true, null);
      }
      else if (g < 4 && binaryOp4(b, l + 1)) {
        r = expression(b, l, 4);
        exit_section_(b, l, m, EXPRESSION_BINARY, r, true, null);
      }
      else if (g < 5 && expressionRegex_0(b, l + 1)) {
        r = true;
        exit_section_(b, l, m, EXPRESSION_REGEX, r, true, null);
      }
      else if (g < 7 && selector_0(b, l + 1)) {
        r = true;
        exit_section_(b, l, m, SELECTOR, r, true, null);
      }
      else if (g < 8 && expressionCall_0(b, l + 1)) {
        r = true;
        exit_section_(b, l, m, EXPRESSION_CALL, r, true, null);
      }
      else if (g < 10 && expressionIndexed_0(b, l + 1)) {
        r = true;
        exit_section_(b, l, m, EXPRESSION_INDEXED, r, true, null);
      }
      else if (g < 12 && leftMarkerIs(b, CAPITALIZED_NAME_WRAPPER) && collection_0(b, l + 1)) {
        r = true;
        exit_section_(b, l, m, COLLECTION, r, true, null);
      }
      else if (g < 12 && leftMarkerIs(b, REGULAR_NAME_WRAPPER) && function_call_expression_0(b, l + 1)) {
        r = true;
        exit_section_(b, l, m, FUNCTION_CALL_EXPRESSION, r, true, null);
      }
      else {
        exit_section_(b, l, m, null, false, false, null);
        break;
      }
    }
    return r;
  }

  // regexOp <<myPin (<<isPuppet4>> expression | regexp)>>
  private static boolean expressionRegex_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "expressionRegex_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = regexOp(b, l + 1);
    r = r && myPinParse(b, l + 1, PuppetParserGenerated::expressionRegex_0_1_0);
    exit_section_(b, m, null, r);
    return r;
  }

  // <<isPuppet4>> expression | regexp
  private static boolean expressionRegex_0_1_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "expressionRegex_0_1_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = expressionRegex_0_1_0_0(b, l + 1);
    if (!r) r = regexp(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // <<isPuppet4>> expression
  private static boolean expressionRegex_0_1_0_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "expressionRegex_0_1_0_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = isPuppet4(b, l + 1);
    r = r && expression(b, l + 1, -1);
    exit_section_(b, m, null, r);
    return r;
  }

  public static boolean expressionUnary(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "expressionUnary")) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_, null);
    r = expressionUnary_0(b, l + 1);
    p = r;
    r = p && expression(b, l, 6);
    exit_section_(b, l, m, EXPRESSION_UNARY, r, p, null);
    return r || p;
  }

  // '-' | '!' | <<isPuppet4>> '*'
  private static boolean expressionUnary_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "expressionUnary_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeTokenSmart(b, MINUS);
    if (!r) r = consumeTokenSmart(b, NOT);
    if (!r) r = expressionUnary_0_2(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // <<isPuppet4>> '*'
  private static boolean expressionUnary_0_2(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "expressionUnary_0_2")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = isPuppet4(b, l + 1);
    r = r && consumeToken(b, TIMES);
    exit_section_(b, m, null, r);
    return r;
  }

  // '?' selector_content
  private static boolean selector_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "selector_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeTokenSmart(b, QMARK);
    r = r && selector_content(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // <<isPuppet4>> '.' <<myPin (regular_name_wrapper ['(' expressionAssignments? ')'] anonymousBlock?)>>
  private static boolean expressionCall_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "expressionCall_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = isPuppet4(b, l + 1);
    r = r && consumeToken(b, DOT);
    r = r && myPinParse(b, l + 1, PuppetParserGenerated::expressionCall_0_2_0);
    exit_section_(b, m, null, r);
    return r;
  }

  // regular_name_wrapper ['(' expressionAssignments? ')'] anonymousBlock?
  private static boolean expressionCall_0_2_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "expressionCall_0_2_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = regular_name_wrapper(b, l + 1);
    r = r && expressionCall_0_2_0_1(b, l + 1);
    r = r && expressionCall_0_2_0_2(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // ['(' expressionAssignments? ')']
  private static boolean expressionCall_0_2_0_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "expressionCall_0_2_0_1")) return false;
    expressionCall_0_2_0_1_0(b, l + 1);
    return true;
  }

  // '(' expressionAssignments? ')'
  private static boolean expressionCall_0_2_0_1_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "expressionCall_0_2_0_1_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeTokenSmart(b, LPAREN);
    r = r && expressionCall_0_2_0_1_0_1(b, l + 1);
    r = r && consumeToken(b, RPAREN);
    exit_section_(b, m, null, r);
    return r;
  }

  // expressionAssignments?
  private static boolean expressionCall_0_2_0_1_0_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "expressionCall_0_2_0_1_0_1")) return false;
    expressionAssignments(b, l + 1);
    return true;
  }

  // anonymousBlock?
  private static boolean expressionCall_0_2_0_2(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "expressionCall_0_2_0_2")) return false;
    anonymousBlock(b, l + 1);
    return true;
  }

  // <<braced_element hashPairsList?>>
  public static boolean hashValue(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "hashValue")) return false;
    if (!nextTokenIsSmart(b, LBRACE)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = braced_element(b, l + 1, PuppetParserGenerated::hashValue_0_0);
    exit_section_(b, m, HASH_VALUE, r);
    return r;
  }

  // hashPairsList?
  private static boolean hashValue_0_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "hashValue_0_0")) return false;
    hashPairsList(b, l + 1);
    return true;
  }

  // <<isPuppet4>> expressionIndex+
  private static boolean expressionIndexed_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "expressionIndexed_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = isPuppet4(b, l + 1);
    r = r && expressionIndexed_0_1(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // expressionIndex+
  private static boolean expressionIndexed_0_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "expressionIndexed_0_1")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = expressionIndex(b, l + 1);
    while (r) {
      int c = current_position_(b);
      if (!expressionIndex(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "expressionIndexed_0_1", c)) break;
    }
    exit_section_(b, m, null, r);
    return r;
  }

  // <<parenthesized_element expressionAssignment>>
  public static boolean expressionParen(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "expressionParen")) return false;
    if (!nextTokenIsSmart(b, LPAREN)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = parenthesized_element(b, l + 1, PuppetParserGenerated::expressionAssignment);
    exit_section_(b, m, EXPRESSION_PAREN, r);
    return r;
  }

  // collectRHand braced_any_arguments_list_block?
  private static boolean collection_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "collection_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = collectRHand(b, l + 1);
    r = r && collection_0_1(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // braced_any_arguments_list_block?
  private static boolean collection_0_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "collection_0_1")) return false;
    braced_any_arguments_list_block(b, l + 1);
    return true;
  }

  // hashAccessSource hash_array_index+
  public static boolean hashArrayAccesses(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "hashArrayAccesses")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, HASH_ARRAY_ACCESSES, "<hash array accesses>");
    r = hashAccessSource(b, l + 1);
    r = r && hashArrayAccesses_1(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  // hash_array_index+
  private static boolean hashArrayAccesses_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "hashArrayAccesses_1")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = hash_array_index(b, l + 1);
    while (r) {
      int c = current_position_(b);
      if (!hash_array_index(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "hashArrayAccesses_1", c)) break;
    }
    exit_section_(b, m, null, r);
    return r;
  }

  // <<bracketed_element expressions_list_with_tailing_comma?>> | puppet4_array
  public static boolean array(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "array")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, ARRAY, "<array>");
    r = bracketed_element(b, l + 1, PuppetParserGenerated::array_0_0);
    if (!r) r = puppet4_array(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  // expressions_list_with_tailing_comma?
  private static boolean array_0_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "array_0_0")) return false;
    expressions_list_with_tailing_comma(b, l + 1);
    return true;
  }

  // {regular_name_wrapper|capitalized_name_wrapper} PRODUCES any_name_wrapper braced_any_arguments_list_block ?
  public static boolean produces_statement(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "produces_statement")) return false;
    if (!nextTokenIsSmart(b, CAPITALIZED_NAME, NAME)) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_, PRODUCES_STATEMENT, "<produces statement>");
    r = produces_statement_0(b, l + 1);
    r = r && consumeToken(b, PRODUCES);
    p = r; // pin = 2
    r = r && report_error_(b, any_name_wrapper(b, l + 1));
    r = p && produces_statement_3(b, l + 1) && r;
    exit_section_(b, l, m, r, p, null);
    return r || p;
  }

  // regular_name_wrapper|capitalized_name_wrapper
  private static boolean produces_statement_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "produces_statement_0")) return false;
    boolean r;
    r = regular_name_wrapper(b, l + 1);
    if (!r) r = capitalized_name_wrapper(b, l + 1);
    return r;
  }

  // braced_any_arguments_list_block ?
  private static boolean produces_statement_3(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "produces_statement_3")) return false;
    braced_any_arguments_list_block(b, l + 1);
    return true;
  }

  // {regular_name_wrapper|capitalized_name_wrapper} CONSUMES any_name_wrapper braced_any_arguments_list_block ?
  public static boolean consumes_statement(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "consumes_statement")) return false;
    if (!nextTokenIsSmart(b, CAPITALIZED_NAME, NAME)) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_, CONSUMES_STATEMENT, "<consumes statement>");
    r = consumes_statement_0(b, l + 1);
    r = r && consumeToken(b, CONSUMES);
    p = r; // pin = 2
    r = r && report_error_(b, any_name_wrapper(b, l + 1));
    r = p && consumes_statement_3(b, l + 1) && r;
    exit_section_(b, l, m, r, p, null);
    return r || p;
  }

  // regular_name_wrapper|capitalized_name_wrapper
  private static boolean consumes_statement_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "consumes_statement_0")) return false;
    boolean r;
    r = regular_name_wrapper(b, l + 1);
    if (!r) r = capitalized_name_wrapper(b, l + 1);
    return r;
  }

  // braced_any_arguments_list_block ?
  private static boolean consumes_statement_3(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "consumes_statement_3")) return false;
    braced_any_arguments_list_block(b, l + 1);
    return true;
  }

  // <<isPuppet3>> regular_name_wrapper data_type_parameters_block
  //   | capitalized_name_wrapper [<<checkNoSpaceBefore>> data_type_parameters_block]
  public static boolean data_type(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "data_type")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _COLLAPSE_, DATA_TYPE, "<data type>");
    r = data_type_0(b, l + 1);
    if (!r) r = data_type_1(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  // <<isPuppet3>> regular_name_wrapper data_type_parameters_block
  private static boolean data_type_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "data_type_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = isPuppet3(b, l + 1);
    r = r && regular_name_wrapper(b, l + 1);
    r = r && data_type_parameters_block(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // capitalized_name_wrapper [<<checkNoSpaceBefore>> data_type_parameters_block]
  private static boolean data_type_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "data_type_1")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = capitalized_name_wrapper(b, l + 1);
    r = r && data_type_1_1(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // [<<checkNoSpaceBefore>> data_type_parameters_block]
  private static boolean data_type_1_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "data_type_1_1")) return false;
    data_type_1_1_0(b, l + 1);
    return true;
  }

  // <<checkNoSpaceBefore>> data_type_parameters_block
  private static boolean data_type_1_1_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "data_type_1_1_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = checkNoSpaceBefore(b, l + 1);
    r = r && data_type_parameters_block(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // TRUE
  public static boolean expressionTrue(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "expressionTrue")) return false;
    if (!nextTokenIsSmart(b, TRUE)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeTokenSmart(b, TRUE);
    exit_section_(b, m, EXPRESSION_TRUE, r);
    return r;
  }

  // FALSE
  public static boolean expressionFalse(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "expressionFalse")) return false;
    if (!nextTokenIsSmart(b, FALSE)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeTokenSmart(b, FALSE);
    exit_section_(b, m, EXPRESSION_FALSE, r);
    return r;
  }

  // DOLLAR { VARIABLE_NAME | VARIABLE_LBRACE VARIABLE_NAME VARIABLE_RBRACE }
  public static boolean varWrapper(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "varWrapper")) return false;
    if (!nextTokenIsSmart(b, DOLLAR)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeTokenSmart(b, DOLLAR);
    r = r && varWrapper_1(b, l + 1);
    exit_section_(b, m, VAR_WRAPPER, r);
    return r;
  }

  // VARIABLE_NAME | VARIABLE_LBRACE VARIABLE_NAME VARIABLE_RBRACE
  private static boolean varWrapper_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "varWrapper_1")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeTokenSmart(b, VARIABLE_NAME);
    if (!r) r = parseTokensSmart(b, 0, VARIABLE_LBRACE, VARIABLE_NAME, VARIABLE_RBRACE);
    exit_section_(b, m, null, r);
    return r;
  }

  // parenthesized_expressions_list_block anonymousBlock?
  private static boolean function_call_expression_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "function_call_expression_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = parenthesized_expressions_list_block(b, l + 1);
    r = r && function_call_expression_0_1(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // anonymousBlock?
  private static boolean function_call_expression_0_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "function_call_expression_0_1")) return false;
    anonymousBlock(b, l + 1);
    return true;
  }

  // SINGLE_QUOTED_STRING | DOUBLE_QUOTED_STRING | stringTemplate | heredoc_body
  public static boolean quoted_text(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "quoted_text")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _COLLAPSE_, QUOTED_TEXT, "<quoted text>");
    r = consumeTokenSmart(b, SINGLE_QUOTED_STRING);
    if (!r) r = consumeTokenSmart(b, DOUBLE_QUOTED_STRING);
    if (!r) r = stringTemplate(b, l + 1);
    if (!r) r = heredoc_body(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  // INTEGER_LITERAL_WITHOUTQ | FLOAT_LITERAL
  public static boolean expressionNumeric(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "expressionNumeric")) return false;
    if (!nextTokenIsSmart(b, FLOAT_LITERAL, INTEGER_LITERAL_WITHOUTQ)) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, EXPRESSION_NUMERIC, "<expression numeric>");
    r = consumeTokenSmart(b, INTEGER_LITERAL_WITHOUTQ);
    if (!r) r = consumeTokenSmart(b, FLOAT_LITERAL);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  // HEREDOC_AT '(' HEREDOC_END_TAG HEREDOC_SYNTAX? HEREDOC_ESCAPES? ')'
  public static boolean heredocExpression(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "heredocExpression")) return false;
    if (!nextTokenIsSmart(b, HEREDOC_AT)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeTokensSmart(b, 0, HEREDOC_AT, LPAREN, HEREDOC_END_TAG);
    r = r && heredocExpression_3(b, l + 1);
    r = r && heredocExpression_4(b, l + 1);
    r = r && consumeToken(b, RPAREN);
    exit_section_(b, m, HEREDOC_EXPRESSION, r);
    return r;
  }

  // HEREDOC_SYNTAX?
  private static boolean heredocExpression_3(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "heredocExpression_3")) return false;
    consumeTokenSmart(b, HEREDOC_SYNTAX);
    return true;
  }

  // HEREDOC_ESCAPES?
  private static boolean heredocExpression_4(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "heredocExpression_4")) return false;
    consumeTokenSmart(b, HEREDOC_ESCAPES);
    return true;
  }

  // REGEX
  public static boolean regexp(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "regexp")) return false;
    if (!nextTokenIsSmart(b, REGEX)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeTokenSmart(b, REGEX);
    exit_section_(b, m, REGEXP, r);
    return r;
  }

  // UNDEF
  public static boolean expressionUndef(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "expressionUndef")) return false;
    if (!nextTokenIsSmart(b, UNDEF)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeTokenSmart(b, UNDEF);
    exit_section_(b, m, EXPRESSION_UNDEF, r);
    return r;
  }

  // DEFAULT
  public static boolean default_wrapper(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "default_wrapper")) return false;
    if (!nextTokenIsSmart(b, DEFAULT)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeTokenSmart(b, DEFAULT);
    exit_section_(b, m, DEFAULT_WRAPPER, r);
    return r;
  }

  // CASE expression  braced_case_opts_block
  public static boolean case_expression(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "case_expression")) return false;
    if (!nextTokenIsSmart(b, CASE)) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_, CASE_EXPRESSION, null);
    r = consumeTokenSmart(b, CASE);
    p = r; // pin = 1
    r = r && report_error_(b, expression(b, l + 1, -1));
    r = p && braced_case_opts_block(b, l + 1) && r;
    exit_section_(b, l, m, r, p, null);
    return r || p;
  }

  // IF conditional_branch elsif_branch * else_branch ?
  public static boolean if_statement(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "if_statement")) return false;
    if (!nextTokenIsSmart(b, IF)) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_, IF_STATEMENT, null);
    r = consumeTokenSmart(b, IF);
    p = r; // pin = 1
    r = r && report_error_(b, conditional_branch(b, l + 1));
    r = p && report_error_(b, if_statement_2(b, l + 1)) && r;
    r = p && if_statement_3(b, l + 1) && r;
    exit_section_(b, l, m, r, p, null);
    return r || p;
  }

  // elsif_branch *
  private static boolean if_statement_2(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "if_statement_2")) return false;
    while (true) {
      int c = current_position_(b);
      if (!elsif_branch(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "if_statement_2", c)) break;
    }
    return true;
  }

  // else_branch ?
  private static boolean if_statement_3(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "if_statement_3")) return false;
    else_branch(b, l + 1);
    return true;
  }

  // UNLESS conditional_branch [<<isPuppet4>> else_branch]
  public static boolean unless_statement(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "unless_statement")) return false;
    if (!nextTokenIsSmart(b, UNLESS)) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_, UNLESS_STATEMENT, null);
    r = consumeTokenSmart(b, UNLESS);
    p = r; // pin = 1
    r = r && report_error_(b, conditional_branch(b, l + 1));
    r = p && unless_statement_2(b, l + 1) && r;
    exit_section_(b, l, m, r, p, null);
    return r || p;
  }

  // [<<isPuppet4>> else_branch]
  private static boolean unless_statement_2(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "unless_statement_2")) return false;
    unless_statement_2_0(b, l + 1);
    return true;
  }

  // <<isPuppet4>> else_branch
  private static boolean unless_statement_2_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "unless_statement_2_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = isPuppet4(b, l + 1);
    r = r && else_branch(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // IMPORT stringsList
  public static boolean import_statement(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "import_statement")) return false;
    if (!nextTokenIsSmart(b, IMPORT)) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_, IMPORT_STATEMENT, null);
    r = consumeTokenSmart(b, IMPORT);
    p = r; // pin = 1
    r = r && stringsList(b, l + 1);
    exit_section_(b, l, m, r, p, null);
    return r || p;
  }

  // SITE braced_statements_block
  public static boolean site_compound(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "site_compound")) return false;
    if (!nextTokenIsSmart(b, SITE)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeTokenSmart(b, SITE);
    r = r && braced_statements_block(b, l + 1);
    exit_section_(b, m, SITE_COMPOUND, r);
    return r;
  }

  // (DEFINE|APPLICATION) fqn_container parenthesized_parameters_list_block? braced_statements_block
  public static boolean type_definition(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "type_definition")) return false;
    if (!nextTokenIsSmart(b, APPLICATION, DEFINE)) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_, TYPE_DEFINITION, "<type definition>");
    r = type_definition_0(b, l + 1);
    p = r; // pin = 1
    r = r && report_error_(b, fqn_container(b, l + 1));
    r = p && report_error_(b, type_definition_2(b, l + 1)) && r;
    r = p && braced_statements_block(b, l + 1) && r;
    exit_section_(b, l, m, r, p, null);
    return r || p;
  }

  // DEFINE|APPLICATION
  private static boolean type_definition_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "type_definition_0")) return false;
    boolean r;
    r = consumeTokenSmart(b, DEFINE);
    if (!r) r = consumeTokenSmart(b, APPLICATION);
    return r;
  }

  // parenthesized_parameters_list_block?
  private static boolean type_definition_2(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "type_definition_2")) return false;
    parenthesized_parameters_list_block(b, l + 1);
    return true;
  }

  // CLASS fqn_container parenthesized_parameters_list_block? class_inheritance? braced_statements_block
  public static boolean class_definition(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "class_definition")) return false;
    if (!nextTokenIsSmart(b, CLASS)) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_, CLASS_DEFINITION, null);
    r = consumeTokenSmart(b, CLASS);
    p = r; // pin = 1
    r = r && report_error_(b, fqn_container(b, l + 1));
    r = p && report_error_(b, class_definition_2(b, l + 1)) && r;
    r = p && report_error_(b, class_definition_3(b, l + 1)) && r;
    r = p && braced_statements_block(b, l + 1) && r;
    exit_section_(b, l, m, r, p, null);
    return r || p;
  }

  // parenthesized_parameters_list_block?
  private static boolean class_definition_2(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "class_definition_2")) return false;
    parenthesized_parameters_list_block(b, l + 1);
    return true;
  }

  // class_inheritance?
  private static boolean class_definition_3(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "class_definition_3")) return false;
    class_inheritance(b, l + 1);
    return true;
  }

  // NODE node_names_list parent_node? braced_statements_block
  public static boolean node_definition(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "node_definition")) return false;
    if (!nextTokenIsSmart(b, NODE)) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_, NODE_DEFINITION, null);
    r = consumeTokenSmart(b, NODE);
    p = r; // pin = 1
    r = r && report_error_(b, node_names_list(b, l + 1));
    r = p && report_error_(b, node_definition_2(b, l + 1)) && r;
    r = p && braced_statements_block(b, l + 1) && r;
    exit_section_(b, l, m, r, p, null);
    return r || p;
  }

  // parent_node?
  private static boolean node_definition_2(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "node_definition_2")) return false;
    parent_node(b, l + 1);
    return true;
  }

  // FUNCTION fqn_container parenthesized_parameters_list_block ? function_return_type ? braced_statements_block
  public static boolean function_definition(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "function_definition")) return false;
    if (!nextTokenIsSmart(b, FUNCTION)) return false;
    boolean r, p;
    Marker m = enter_section_(b, l, _NONE_, FUNCTION_DEFINITION, null);
    r = consumeTokenSmart(b, FUNCTION);
    p = r; // pin = 1
    r = r && report_error_(b, fqn_container(b, l + 1));
    r = p && report_error_(b, function_definition_2(b, l + 1)) && r;
    r = p && report_error_(b, function_definition_3(b, l + 1)) && r;
    r = p && braced_statements_block(b, l + 1) && r;
    exit_section_(b, l, m, r, p, null);
    return r || p;
  }

  // parenthesized_parameters_list_block ?
  private static boolean function_definition_2(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "function_definition_2")) return false;
    parenthesized_parameters_list_block(b, l + 1);
    return true;
  }

  // function_return_type ?
  private static boolean function_definition_3(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "function_definition_3")) return false;
    function_return_type(b, l + 1);
    return true;
  }

  // NAME
  public static boolean regular_name_wrapper(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "regular_name_wrapper")) return false;
    if (!nextTokenIsSmart(b, NAME)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeTokenSmart(b, NAME);
    exit_section_(b, m, REGULAR_NAME_WRAPPER, r);
    return r;
  }

  static final Parser expression_parser_ = (b, l) -> expression(b, l + 1, -1);

  private static final Parser braced_resource_by_classname_contents_block_0_0_parser_ = resource_like_declarations_list_$(PuppetParserGenerated::resource_instance_declaration);
  private static final Parser resource_like_class_declaration_block_0_0_parser_ = resource_like_declarations_list_$(PuppetParserGenerated::resource_like_class_description);
}
