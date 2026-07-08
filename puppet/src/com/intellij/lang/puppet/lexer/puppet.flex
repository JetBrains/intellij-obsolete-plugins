package com.intellij.lang.puppet.lexer;

import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.project.Project;
import com.intellij.lang.puppet.PuppetLanguage;
import com.intellij.psi.tree.IElementType;

import static com.intellij.psi.TokenType.WHITE_SPACE;

%%
%unicode
%public
%class _PuppetLexer
%extends PuppetLexerBase
%function advance
%type IElementType
//Under construction
%{
  public _PuppetLexer(Project project) {
    super(project);
    this.zzReader = null;
  }
  public void setTokenEnd(int position){zzMarkedPos = position;}
%}

////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
////////// whitespaces and comments ////////////////////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

EOL =                               \R
WHITE_SPACE_CHAR =                  [ \r\t]
WHITE_SPACE =                       {WHITE_SPACE_CHAR}+
ANY_CHAR =                          .
LINE =                              {ANY_CHAR}*
LINE_COMMENT =                      "#"{LINE}
BLOCK_COMMENT =                     ("/*"{COMMENT_TAIL})|"/*"
COMMENT_TAIL =                      ([^"*"]*("*"+[^"*""/"])?)*("*"+"/")?
COMMENT =                           {LINE_COMMENT} | {BLOCK_COMMENT}

// this rule is a bit weak. Actually it can be any meanless block: space, comment or newline; But newline requires heredoc handling which is more complicated
MEANINGLESS_BLOCK = ({WHITE_SPACE_CHAR}|{EOL}|{COMMENT})*

////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
///////////////////// integers /////////////////////////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
DIGIT =                             [0-9]
DIGITS =                            {DIGIT} | {DIGIT}"_"{DIGIT}
DECIMAL_INTEGER_LITERAL =           {DIGITS}+

OCTAL_DIGIT =                       [0-7]
OCTAL_DIGITS =                      {OCTAL_DIGIT} | {OCTAL_DIGIT}"_"{OCTAL_DIGIT}
OCTAL_INTEGER_LITERAL =             [0-7]{OCTAL_DIGITS}*

BINARY_DIGIT =                      [0-1]
BINARY_DIGITS =                     {BINARY_DIGIT}|{BINARY_DIGIT}"_"{BINARY_DIGIT}
BINARY_INTEGER_LITERAL =            "0"[Bb]{BINARY_DIGITS}*

HEX_DIGIT =                         [0-9A-Fa-f]
HEX_DIGITS =                        {HEX_DIGIT}|{HEX_DIGIT}"_"{HEX_DIGIT}
HEX_INTEGER_LITERAL =               "0"[Xx]{HEX_DIGITS}*

FARROW = "=>"
PARROW = "+>"
ARROW = {FARROW}|{PARROW}

INTEGER_LITERAL_WITHOUTQ =          {DECIMAL_INTEGER_LITERAL} | {HEX_INTEGER_LITERAL} | {OCTAL_INTEGER_LITERAL} | {BINARY_INTEGER_LITERAL}

////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//////////////////// floating point ////////////////////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
FLOATING_POINT_LITERAL1 =       {DIGITS}*"."{DIGITS}+{EXPONENT_PART}?
FLOATING_POINT_LITERAL2 =       {DIGITS}+{EXPONENT_PART}
EXPONENT_PART =                 [Ee][+-]?{DIGITS}+
FLOAT_LITERAL =                 {FLOATING_POINT_LITERAL1} | {FLOATING_POINT_LITERAL2}

NAMESPACE_SEPARATOR="::"

// following pattern must be the same as com.intellij.lang.puppet.lexer.PuppetLexerBase.IDENTIFIER_PATTERN
IDENTIFIER = [a-z][\-a-zA-Z0-9_]*

BAREWORD = {NAMESPACE_SEPARATOR}?[a-z_]([\w-]*[\w])?
NAME={NAMESPACE_SEPARATOR}? {IDENTIFIER} ({NAMESPACE_SEPARATOR}{IDENTIFIER})*
CAPITALIZED_NAME={NAMESPACE_SEPARATOR}?[A-Z][\-a-zA-Z0-9_]*({NAMESPACE_SEPARATOR}[A-Z][\-a-zA-Z0-9_]*)*
VARIABLE_BODY={NAMESPACE_SEPARATOR}?[a-zA-Z0-9_]+({NAMESPACE_SEPARATOR}[a-zA-Z0-9_]+)*
BRACED_VARIABLE_BODY = "{" {VARIABLE_BODY} "}"

REGEX = "/"([^/\n]| \\\/ )*"/"

////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//////////////////// Strings and Regexps////////////////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

ESCAPE_SEQUENCE=\\[^\r\n]
// v4 sq_string, v3 limitations should be checked in annotator
SQ_ESCAPE_SEQUENCE= "\\"[^]
SINGLE_QUOTED_STRING=\'({SQ_ESCAPE_SEQUENCE}|[^\'\\])* \' ?

DOUBLE_QUOTED_STRING_PART=[^\"\\$] | {ESCAPE_SEQUENCE} | \\\n | \$([^a-zA-Z0-9_\{\"] | :[^:] | \\\\ | \\\")
DOUBLE_QUOTED_STRING=\"{DOUBLE_QUOTED_STRING_PART}*\"
DOUBLE_QUOTED_STRING_START=\"{DOUBLE_QUOTED_STRING_PART}*
DOUBLE_QUOTED_STRING_MIDDLE={DOUBLE_QUOTED_STRING_PART}+
DOUBLE_QUOTED_STRING_END={DOUBLE_QUOTED_STRING_PART}*"$"?\"
VAR_INTERPOLATION_START=\$\{

HEREDOC_END_TAG=\"(.*)\" | [^:\/\r\n\()]+
HEREDOC_SYNTAX=[a-z][a-zA-Z_+]+
HEREDOC_ESCAPES="t" | "r" | "n" | "s" | "u" | "L" | "$"
HEREDOC_START="@" "("
HEREDOC_EXPRESSION={HEREDOC_START}{HEREDOC_END_TAG} (":" {HEREDOC_SYNTAX})? ("/" {HEREDOC_ESCAPES}*)? ")"
HEREDOC_ENDING={WHITE_SPACE}? ("|" {WHITE_SPACE}?)? ("-" {WHITE_SPACE}?)? {HEREDOC_END_TAG} {WHITE_SPACE}?

//States
%state IN_DQ_STRING, AFTER_LITERAL, HEREDOC_DEF, DEFAULT_NOT_INIT
%state LEX_VARIABLE, LEX_BRACED_VARIABLE
%state LEX_FQN
%state AFTER_KEYWORD

%xstate IN_HEREDOC,IN_NON_EMPTY_HEREDOC
%xstate HEREDOC_QQ

%%

///////////////////////////////// heredoc re-lex and interpolated string content ///////////////////////////////////////////////////////////

<HEREDOC_QQ, IN_DQ_STRING>{
  "$" / {VARIABLE_BODY}              { pushStateAndBegin(LEX_VARIABLE); return DOLLAR; }
  "$" / {BRACED_VARIABLE_BODY}       { pushStateAndBegin(LEX_BRACED_VARIABLE); return DOLLAR; }
  {VAR_INTERPOLATION_START}          { pushStateAndBegin(YYINITIAL); return VAR_INTERPOLATION_START;}
}

<HEREDOC_QQ> {
  [^$]+                              { return DOUBLE_QUOTED_STRING_MIDDLE; }
  "$"                                { return DOUBLE_QUOTED_STRING_MIDDLE; }
}

<IN_DQ_STRING> {
  {DOUBLE_QUOTED_STRING_MIDDLE} { return DOUBLE_QUOTED_STRING_MIDDLE; }
  {DOUBLE_QUOTED_STRING_END}    { popState(); return DOUBLE_QUOTED_STRING_END; }
  .                             { return ANY_CHAR; }
}

///////////////////////////////// heredoc capture //////////////////////////////////////////////////////////////////////////////////////////

<IN_NON_EMPTY_HEREDOC>{
  {HEREDOC_ENDING}  {
    if( isCurrentHeredocCloser()){
      pushback();
      yybegin(IN_HEREDOC);
      return myHeredocQueue.peekFirst().myInterpolationAllowed ? HEREDOC_BODY_QQ: HEREDOC_BODY;
    }
  }
  .+      {}
  {EOL}+  {}
  <<EOF>> {popState();return myHeredocQueue.peekFirst().myInterpolationAllowed ? HEREDOC_BODY_QQ: HEREDOC_BODY;}
}

<IN_HEREDOC> {
  {HEREDOC_ENDING}  {
    if (isCurrentHeredocCloser()) {
      myHeredocQueue.removeFirst();
      popState();
      return HEREDOC_ENDING;
    }
    yybegin(IN_NON_EMPTY_HEREDOC);
  }
  .+                        { yybegin(IN_NON_EMPTY_HEREDOC); }
  {EOL}+                    { yybegin(IN_NON_EMPTY_HEREDOC); }
}

///////////////////////////////// end of heredoc capture ///////////////////////////////////////////////////////////////////////////////////

// there works for any rule except strings and heredocs
{EOL} {
  setLookLikeIncompleteHeredocOpenerLine(false);
  if (!myHeredocQueue.isEmpty()) {
    pushStateAndBegin(IN_HEREDOC);
  }
  return WHITE_SPACE;
}
{WHITE_SPACE} { return WHITE_SPACE; }


<LEX_FQN>
{
  {NAMESPACE_SEPARATOR}                 {return NAMESPACE_SEPARATOR;}
  {IDENTIFIER} / {NAMESPACE_SEPARATOR}  {return NAME;}
  {IDENTIFIER}                          {myYyBegin(YYINITIAL); return NAME;}
  [^]                                   {pushback();myYyBegin(YYINITIAL);break;}
}

<LEX_VARIABLE>{
  {VARIABLE_BODY} {popState();return VARIABLE_NAME;}
}

<LEX_BRACED_VARIABLE>
{
  "{"             {return VARIABLE_LBRACE;}
  {VARIABLE_BODY} {return VARIABLE_NAME;}
  "}"             {popState();return VARIABLE_RBRACE;}
}


<HEREDOC_DEF> {
  "(" { return LPAREN; }
  ")" { myYyBegin(YYINITIAL); return RPAREN; }
  {HEREDOC_END_TAG} {
    String endTag = yytext().toString();
    boolean isInterpolationAllowed = endTag.startsWith("\"");
    myHeredocQueue.addLast(new HeredocInfo(StringUtil.unquoteString(endTag), isInterpolationAllowed));
    return HEREDOC_END_TAG;
  }
  ":" {HEREDOC_SYNTAX} { return HEREDOC_SYNTAX; }
  "/" {HEREDOC_ESCAPES} { return HEREDOC_ESCAPES; }
}

<AFTER_LITERAL> "/" { return DIV;}
<AFTER_LITERAL> . { myYyBegin(YYINITIAL); yypushback(1); }

<YYINITIAL, DEFAULT_NOT_INIT, AFTER_LITERAL,AFTER_KEYWORD>{
  {COMMENT} { return COMMENT; }
}

<AFTER_KEYWORD> [^] {yypushback(1);myYyBegin(YYINITIAL);}

<YYINITIAL, DEFAULT_NOT_INIT> {

//Variables inteprolation
  {DOUBLE_QUOTED_STRING} { return DOUBLE_QUOTED_STRING; }
  {SINGLE_QUOTED_STRING} { return SINGLE_QUOTED_STRING; }
  {DOUBLE_QUOTED_STRING_START}     { pushStateAndBegin(IN_DQ_STRING); return DOUBLE_QUOTED_STRING_START;}

  {HEREDOC_EXPRESSION} {
    if (getLanguageVersion() == PuppetLanguage.Version.PUPPET_4) {
      myYyBegin(HEREDOC_DEF);
    }
    yypushback(yylength() - 1);
    return HEREDOC_AT;
  }
  {HEREDOC_START} {
    setLookLikeIncompleteHeredocOpenerLine(true);
    yypushback(yylength() - 1);
    return AT;
  }

//Regular expressions
  {ESCAPE_SEQUENCE}            { return ESCAPE_SEQUENCE;}
  "$" / {VARIABLE_BODY} {
    myYyBegin(AFTER_LITERAL);
    pushStateAndBegin(LEX_VARIABLE);
    return DOLLAR;
  }
  {CAPITALIZED_NAME} { return proxyLiteralToken(CAPITALIZED_NAME); }

  // default might be in selectors as a keyword
  "default" / {MEANINGLESS_BLOCK}{ARROW} {return DEFAULT;}
  {BAREWORD} / {MEANINGLESS_BLOCK}{ARROW} {return NAME;}

  {NAME} { return getNameOrKeywordToken(); }

//operators
  "[" {return lexLBrack(); }
  "]" { return proxyLiteralToken(RBRACK); }
  "{" { return LBRACE; }
  "}" {
        if (stack.size() == 0)
         return RBRACE;
        else {
         popState();
         return VAR_INTERPOLATION_END;
        }
      }
  {FARROW} {return FARROW; }
  {PARROW} {return PARROW; }
  "," {return COMMA; }
  "." {return DOT; }
  "+=" {return APPENDS; }
  "-=" {return DELETES;}
  "!=" {return NOTEQUAL; }
  "::" {return NAMESPACE_SEPARATOR; }
  ":" {return COLON; }
  "<<|" {return LLCOLLECT; }
  "|>>" {return RRCOLLECT;}
  "<|" {return LCOLLECT;}
  "|>" {return RCOLLECT;}
  "?" {return QMARK;}
  "(" {return LPAREN;}
  ")" { return proxyLiteralToken(RPAREN);}
  "==" {return ISEQUAL;}
  ">=" {return GREATEREQUAL;}
  "<=" {return LESSEQUAL; }
  "@@" {return ATAT;}
  "@" {return AT;}
  ";" {return SEMIC;}
  "<<" {return LSHIFT;}
  ">>" {return RSHIFT;}
  "=~" {return MATCH;}
  "!~" {return NOMATCH;}
  "<-" {return OUT_EDGE;}
  "->" {return IN_EDGE;}
  "~>" {return IN_EDGE_SUB;}
  "<~" {return OUT_EDGE_SUB;}
  "=" {return EQUALS; }
  ">" {return GREATERTHAN;}
  "<" {return LESSTHAN;}
  "+" {return PLUS;}
  "-" {return MINUS;}
  "*" {return TIMES;}
  "!" {return NOT;}
  "^" {return UP;}
  "|" {return PIPE;}
  "%" {return MODULO;}
  {REGEX} { return REGEX; }
  {INTEGER_LITERAL_WITHOUTQ}  {  return proxyLiteralToken(INTEGER_LITERAL_WITHOUTQ); }
  {FLOAT_LITERAL}             {  return proxyLiteralToken(FLOAT_LITERAL); }
  . { return ANY_CHAR; }
}