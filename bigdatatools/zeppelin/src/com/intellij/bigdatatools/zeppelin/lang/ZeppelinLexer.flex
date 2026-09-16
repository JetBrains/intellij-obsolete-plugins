package com.intellij.bigdatatools.notebooks.zeppelin.lang;
import com.intellij.psi.tree.IElementType;
import com.intellij.lexer.FlexLexer;

import static com.intellij.psi.TokenType.BAD_CHARACTER;
import static com.intellij.bigdatatools.zeppelin.psi.ZeppelinTemplateTypes.NEWLINE;
import static com.intellij.bigdatatools.notebooks.zeppelin.psi.ZeppelinTypes.*;

%%

%{
  public _ZeppelinLexer() {
    this((java.io.Reader)null);
  }
%}

%public
%class _ZeppelinLexer
%implements FlexLexer
%function advance
%type IElementType
%unicode

NEWLINE=\n
DELIMETER = %##
OPTIONAL_INTERPRETER_PREFIX = ((\S+)\.)?
MARKER_TAIL = [\t| ]{0,1}

CODE_MARKER=\n%\S*{MARKER_TAIL}

ANY=[^\n|](.*)
ANY_HEADER=([^\n]*)

%state CODE_MARKER_STATE
%state BODY_OR_NEW_CELL


%%
<YYINITIAL> {
  {DELIMETER}      { yybegin(CODE_MARKER_STATE); }
}

<CODE_MARKER_STATE> {
  {DELIMETER}      { yypushback(yylength()); yybegin(YYINITIAL); return CODE_MARKER; }
  {CODE_MARKER}      { yybegin(BODY_OR_NEW_CELL); return CODE_MARKER; }
  {ANY}       { yypushback(yylength()); yybegin(BODY_OR_NEW_CELL); return CODE_MARKER; }
  {NEWLINE}       { yypushback(yylength()); yybegin(BODY_OR_NEW_CELL);return CODE_MARKER; }
//  [\t| ]{0,1}  { return CODE_MARKER; }
}

<BODY_OR_NEW_CELL> {
  {DELIMETER}    { yypushback(yylength()); yybegin(YYINITIAL); }
  {NEWLINE}          { return NEWLINE; }
  {ANY}              { return ANY; }
}

[^] { return BAD_CHARACTER; }