 /* It's an automatically generated code. Do not modify it. */
package com.intellij.play.language.lexer;

import static com.intellij.play.language.PlayElementTypes.*;
import com.intellij.play.language.*;
import com.intellij.lexer.*;
import com.intellij.psi.tree.IElementType;

%%

%{

  public _PlayLexer() {
    this((java.io.Reader)null);
  }
%}

%public
%class _PlayLexer
%implements FlexLexer
%function advance
%type IElementType
%unicode

%state NO_SPACE
                                         
ALPHA=[:letter:]|[_@]
ALPHA_NUMERIC={ALPHA}|[:digit:]
IDENT={ALPHA}({ALPHA_NUMERIC})*
NUMBER=[:digit:]+
WS=[\ \n\r\t\f]
WHITE_SPACE={WS}+

%%

\< / #\{ { return LT; }
\[ / #\{ { return LEFT_BRACKET; }

// expressions ${...}
(\$\{)  { return PlayElementTypes.EL_START; }


// Tags: #{tagName /}
(\#\{)   { return PlayElementTypes.TAG_START; }
(\#\{\/) { return PlayElementTypes.END_TAG_START; }
(\/\})   { return PlayElementTypes.CLOSE_TAG; }

  // Actions: @{…} or @@{…}
(\@\{)   { return PlayElementTypes.ACTION_START; }
(\@\@\{) { return PlayElementTypes.ACTION_DOUBLE_START; }
(\@) { return PlayElementTypes.AT; }
  // Messages: &{…}
(\&\{) { return PlayElementTypes.MESSAGE_START; }


<NO_SPACE> {WHITE_SPACE} { yybegin(YYINITIAL); return PlayElementTypes.TERMINATING_WHITE_SPACE; }
{WHITE_SPACE} { return PlayElementTypes.WHITE_SPACE; }
\.\. { return RANGE; }

\" { return DOUBLE_QUOTE; }
\' { return SINGLE_QUOTE; }
\?\? { return DOUBLE_QUESTION; }
\? { return QUESTION; }

&& { return PlayElementTypes.AND; }
\|\| { return PlayElementTypes.OR; }
\! { return PlayElementTypes.EXCLAM; }

=(=)? { return PlayElementTypes.EQ; }
\!= { return PlayElementTypes.NEQ; }
\<|&lt;|\\lt|lt { return LT; }
> { return JUST_GT; }
&gt;|\\gt|gt { return GT; }
\<=|&lt;=|\\lte|lte { return LTE; }
&gt;=|\\gte|gte { return GTE; }

\[ { return LEFT_BRACKET; }
\] { return RIGHT_BRACKET; }
\{ { return LEFT_BRACE; }
\} { return RIGHT_BRACE; }
\( { return LEFT_PAREN; }
\) { return RIGHT_PAREN; }

\, { return PlayElementTypes.COMMA; }
\: { return PlayElementTypes.COLON; }
\; { return PlayElementTypes.SEMICOLON; }
\$ { return PlayElementTypes.DOLLAR; }
\# { return PlayElementTypes.SHARP; }
\% { return PlayElementTypes.PERCENT; }
\* { return PlayElementTypes.ASTERISK; }

true|false { return PlayElementTypes.BOOLEAN; }

\\\"|\\'|\\\\|\\n|\\r|\\t|\\b|\\f|\\l|\\g|\\a|\\\{|\\x([A-fa-f0-9]+) { return PlayElementTypes.CHAR_ESCAPE; }

[^] { return PlayElementTypes.BAD_CHARACTER; }
