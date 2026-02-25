/*
 * Copyright 2000-2007 JetBrains s.r.o.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.plugins.grails.lang.gsp.lexer.core;

import com.intellij.lexer.FlexLexer;
import com.intellij.psi.tree.IElementType;
import java.util.*;
import java.lang.reflect.Field;
import org.jetbrains.annotations.NotNull;
import com.intellij.openapi.util.text.StringUtil;
import org.jetbrains.plugins.grails.lang.gsp.lexer.*;

%%

%class _GspLexer
%implements FlexLexer, GspTokenTypesEx
%unicode
%public

%function advance
%type IElementType

////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
/////////////////////// User code //////////////////////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

%{
  private int curlyCount = 0;

  private CharSequence tagName;
  private CharSequence attrName;

  public boolean isGroovyAttribute() {
    if (attrName != null && "expr".contentEquals(attrName)) {
      if (tagName != null && ("g:collect".contentEquals(tagName) || "g:findAll".contentEquals(tagName))) {
        return true;
      }
    }

    return false;
  }
%}


////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
////////// GSP comments ////////////////////////////////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

J_COMMENT_BEGIN = "<%--"
G_COMMENT_BEGIN = "%{--"

////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
////////// Script injection delimiters /////////////////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

JSCRIPT_BEGIN = "<%"
JSCRIPT_END = "%>"
JEXPR_BEGIN = "<%="
JDIRECT_BEGIN = "<%@"
JDECLAR_BEGIN = "<%!"

GEXPR_BEGIN = "${"
GEXPR_END = "}"
GSCRIPT_BEGIN = "%{"
GSCRIPT_END = "}%"
GDIRECT_BEGIN = "@{"
GDIRECT_END = "}"
GDECLAR_BEGIN = "!{"
GDECLAR_END = "}!"

G_INJECTION_START_TAG = {GEXPR_BEGIN} | {GSCRIPT_BEGIN} | {GDIRECT_BEGIN} | {GDECLAR_BEGIN}
INJECTION_START_TAG = {JSCRIPT_BEGIN} | {G_INJECTION_START_TAG}

////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
////////// Groovy custom tags //////////////////////////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

mONE_NL = \r | \n | \r\n                                    // NewLines
mWS = " " | \t | \f | {mONE_NL}                             // Whitespaces

TAGLIB_PREFIX = [a-zA-Z][a-zA-Z_0-9]*

mLETTER = !(!([:jletter:] | "_") | "$")
mIDENT = {mLETTER} ({mLETTER} | [0-9])*

// See GroovyPageParser.populateMapWithAttributes(...)
mATTR_NAME = [^ \t\r\n\'\"=:\$\>\<\/\\]+

////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
////////////////////  states ///////////////////////////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

%xstate INJECTION_START
%xstate IN_J_SCRIPT, IN_JSCRIPT_END
%xstate IN_J_DIRECT, IN_J_DECLAR
%xstate IN_J_EXPR
%xstate IN_GEXPR
%xstate IN_GSCRIPT
%xstate IN_GDIRECT
%xstate IN_GDECLAR

//Grails custom tags
%xstate IN_GTAG_START, IN_GTAG_END, IN_GTAG_END_AFTER_NAME
%xstate ATTR_LIST, ATTR, ATTR_VALUE
%xstate GEXPR_IN_VALUE_DQ, GEXPR_IN_VALUE_SQ, ATTR_VALUE_OR_MAP_DQ, ATTR_VALUE_OR_MAP_SQ, ATTR_VALUE_GSTRING_SQ, ATTR_VALUE_GSTRING_DQ
%xstate ATTR_VALUE_END, ATTR_VALUE_DQ_NOT_MAP, ATTR_VALUE_SQ_NOT_MAP, ATTR_GROOVY_START_DQ, ATTR_GROOVY_START_SQ
%xstate GSP_COMMENT, JSP_COMMENT


%%
<YYINITIAL> {

  {J_COMMENT_BEGIN}                         { yypushback(4);
                                              yybegin(JSP_COMMENT); }

  {G_COMMENT_BEGIN}                         { yypushback(4);
                                              yybegin(GSP_COMMENT); }

//  ({mONE_NL} | {mWS})+                      {  return XML_WHITE_SPACE; }

  \\ "$" | [^]                              {  return GSP_TEMPLATE_DATA; }

  "<" {TAGLIB_PREFIX} ":"                   { yypushback(yytext().length()-1);
                                              yybegin(IN_GTAG_START);
                                              return(GTAG_START_TAG_START); }

  "<""/"{TAGLIB_PREFIX} ":"                 { yypushback(yytext().length()-2);
                                              yybegin(IN_GTAG_END);
                                              return(GTAG_END_TAG_START); }

  {INJECTION_START_TAG}                     {  yybegin(INJECTION_START);
                                               yypushback(2);  }
}
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////  Comments  ////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
<GSP_COMMENT>{
  "--}%"                                    {  yybegin(YYINITIAL); return GSP_STYLE_COMMENT; }
   [^]                                      {  return GSP_STYLE_COMMENT; }
}
<JSP_COMMENT>{
  "--%>"                                    {  yybegin(YYINITIAL); return JSP_STYLE_COMMENT; }
   [^]                                      {  return JSP_STYLE_COMMENT; }
}


////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////  Grails tags  /////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

<IN_GTAG_END> {
  {TAGLIB_PREFIX} ":" [a-zA-Z_0-9\-/]*       { yybegin(IN_GTAG_END_AFTER_NAME);
                                               return GSP_TAG_NAME; }
  [^]                                        { yybegin(YYINITIAL);
                                               return GSP_BAD_CHARACTER; }
}

<IN_GTAG_END_AFTER_NAME> {
  {mWS}+                                     { return GSP_WHITE_SPACE; }
  ">"                                        { yybegin(YYINITIAL);
                                               return GTAG_TAG_END; }
  [^]                                        { yybegin(YYINITIAL);
                                               return GSP_BAD_CHARACTER; }
}

<IN_GTAG_START> {
  {TAGLIB_PREFIX} ":" [a-zA-Z_0-9\-/]* "/"">" { yypushback(2);
                                                tagName = yytext();
                                                yybegin(ATTR_LIST);
                                                return GSP_TAG_NAME; }

  {TAGLIB_PREFIX} ":" [a-zA-Z_0-9\-/]*        { yybegin(ATTR_LIST);
                                                tagName = yytext();
                                                return GSP_TAG_NAME; }

  [^]                                        { yybegin(YYINITIAL);
                                               return GSP_BAD_CHARACTER; }
}

<ATTR_LIST> {
  {mWS}+                                       {  return GSP_WHITE_SPACE; }
  {mATTR_NAME}                                 {  yybegin(ATTR);
                                                  attrName = yytext();
                                                  return GSP_ATTR_NAME; }
  "/>"                                         {  yybegin(YYINITIAL);
                                                  return GTAG_START_TAG_END; }
  ">"                                          {  yybegin(YYINITIAL);
                                                  return GTAG_TAG_END; }
  ("<" | "</")                                 {  yypushback(yytext().length()); yybegin(YYINITIAL); }
  [^]                                          {  return GSP_BAD_CHARACTER; }
}

<ATTR> {
  {mWS}+                                       {  return GSP_WHITE_SPACE; }
  "="                                          {  yybegin(ATTR_VALUE); return GSP_EQ; }
  "/>"                                         {  yybegin(YYINITIAL);
                                                  return GTAG_START_TAG_END; }
  ">"                                          {  yybegin(YYINITIAL);
                                                  return GTAG_TAG_END; }
  "<" | "</"                                   {  yypushback(yytext().length()); yybegin(YYINITIAL); }
  [^]                                          {  yypushback(yytext().length()); yybegin(ATTR_LIST); }
}

<ATTR_VALUE> {

  "/>"                                         {  yybegin(YYINITIAL);
                                                  return GTAG_START_TAG_END; }
  ">"                                          {  yybegin(YYINITIAL);
                                                  return GTAG_TAG_END; }
  "<" | "</"                                   {  yypushback(yytext().length()); yybegin(YYINITIAL); }
  {mWS}+                                       {  return GSP_WHITE_SPACE; }
  \"                                           {  yybegin(isGroovyAttribute() ? ATTR_GROOVY_START_DQ : ATTR_VALUE_OR_MAP_DQ);
                                                  return GSP_ATTR_VALUE_START_DELIMITER; }
  \'                                           {  yybegin(isGroovyAttribute() ? ATTR_GROOVY_START_SQ : ATTR_VALUE_OR_MAP_SQ);
                                                  return GSP_ATTR_VALUE_START_DELIMITER; }
  [^]                                          {  yypushback(yytext().length());  yybegin(ATTR_LIST); }
}

<ATTR_GROOVY_START_DQ> {
  {GEXPR_BEGIN}                                {  curlyCount = 0; yybegin(GEXPR_IN_VALUE_DQ); return GEXPR_BEGIN; }
  [^\$\"] [^\"]* \"                            {  yypushback(1); yybegin(ATTR_VALUE_END); return GROOVY_ATTR_VALUE; }
  \"                                           {  yybegin(ATTR_LIST); return GSP_ATTR_VALUE_END_DELIMITER; }
  [^]                                          {  yybegin(ATTR_LIST); return GSP_BAD_CHARACTER; }
}

<ATTR_GROOVY_START_SQ> {
  {GEXPR_BEGIN}                                {  curlyCount = 0; yybegin(GEXPR_IN_VALUE_SQ); return GEXPR_BEGIN; }
  [^\$\'] [^\']* \'                            {  yypushback(1); yybegin(ATTR_VALUE_END); return GROOVY_ATTR_VALUE; }
  \'                                           {  yybegin(ATTR_LIST); return GSP_ATTR_VALUE_END_DELIMITER; }
  [^]                                          {  yybegin(ATTR_LIST); return GSP_BAD_CHARACTER; }
}

<ATTR_VALUE_OR_MAP_DQ> {
  "[" [^\"]* "]" \"                             {  yypushback(1); yybegin(ATTR_VALUE_END); return GSP_MAP_ATTR_VALUE; }
  {GEXPR_BEGIN}                                 {  curlyCount = 0; yybegin(GEXPR_IN_VALUE_DQ); return GEXPR_BEGIN; }
  \\ "$"                                        {  yybegin(ATTR_VALUE_DQ_NOT_MAP); return GSP_ATTRIBUTE_VALUE_TOKEN; }
  "$"                                           {  yybegin(ATTR_VALUE_GSTRING_DQ); return GSTRING_DOLLAR; }
  \"                                            {  yybegin(ATTR_LIST); return GSP_ATTR_VALUE_END_DELIMITER; }
  [^]                                           {  yybegin(ATTR_VALUE_DQ_NOT_MAP); return GSP_ATTRIBUTE_VALUE_TOKEN; }
}

<ATTR_VALUE_OR_MAP_SQ> {
  "[" [^\']* "]" \'                             {  yypushback(1); yybegin(ATTR_VALUE_END); return GSP_MAP_ATTR_VALUE; }
  {GEXPR_BEGIN}                                 {  curlyCount = 0; yybegin(GEXPR_IN_VALUE_SQ); return GEXPR_BEGIN; }
  \\ "$"                                        {  yybegin(ATTR_VALUE_SQ_NOT_MAP); return GSP_ATTRIBUTE_VALUE_TOKEN; }
  "$"                                           {  yybegin(ATTR_VALUE_GSTRING_SQ); return GSTRING_DOLLAR; }
  \'                                            {  yybegin(ATTR_LIST); return GSP_ATTR_VALUE_END_DELIMITER; }
  [^]                                           {  yybegin(ATTR_VALUE_SQ_NOT_MAP); return GSP_ATTRIBUTE_VALUE_TOKEN; }
}

<GEXPR_IN_VALUE_DQ> {
  "{"                                          {  curlyCount++; return GROOVY_EXPR_CODE; }
  {GEXPR_END}                                  {  if (curlyCount > 0) {
                                                    curlyCount--;
                                                    return GROOVY_EXPR_CODE;
                                                  } else {
                                                    curlyCount = 0;
                                                    yybegin(ATTR_VALUE_DQ_NOT_MAP);
                                                    return GEXPR_END;
                                                  }
                                               }
  [^}]                                         {  return GROOVY_EXPR_CODE; }
}

<ATTR_VALUE_SQ_NOT_MAP> {
  {GEXPR_BEGIN}                                {  curlyCount = 0; yybegin(GEXPR_IN_VALUE_SQ); return GEXPR_BEGIN;}
  \\ "$"                                       {  return GSP_ATTRIBUTE_VALUE_TOKEN; }
  "$"                                          {  yybegin(ATTR_VALUE_GSTRING_SQ); return GSTRING_DOLLAR; }
  [^\']                                        {  return GSP_ATTRIBUTE_VALUE_TOKEN; }
  \'                                           {  yybegin(ATTR_LIST); return GSP_ATTR_VALUE_END_DELIMITER; }
}

<ATTR_VALUE_DQ_NOT_MAP> {
  {GEXPR_BEGIN}                                {  curlyCount = 0; yybegin(GEXPR_IN_VALUE_DQ); return GEXPR_BEGIN;}
  \\ "$"                                       {  return GSP_ATTRIBUTE_VALUE_TOKEN; }
  "$"                                          {  yybegin(ATTR_VALUE_GSTRING_DQ); return GSTRING_DOLLAR; }
  [^\"]                                        {  return GSP_ATTRIBUTE_VALUE_TOKEN; }
  \"                                           {  yybegin(ATTR_LIST); return GSP_ATTR_VALUE_END_DELIMITER; }
}

<ATTR_VALUE_GSTRING_SQ> {
  {mIDENT}                                     {  yybegin(ATTR_VALUE_SQ_NOT_MAP); return GROOVY_ATTR_VALUE; }
  [^]                                          {  yypushback(1); yybegin(ATTR_VALUE_SQ_NOT_MAP); return GSP_ATTRIBUTE_VALUE_TOKEN; }
}

<ATTR_VALUE_GSTRING_DQ> {
  {mIDENT}                                     {  yybegin(ATTR_VALUE_DQ_NOT_MAP); return GROOVY_ATTR_VALUE; }
  [^]                                          {  yypushback(1); yybegin(ATTR_VALUE_DQ_NOT_MAP); return GSP_ATTRIBUTE_VALUE_TOKEN; }
}

<GEXPR_IN_VALUE_SQ> {
  "{"                                          {  curlyCount++; return GROOVY_EXPR_CODE; }
  {GEXPR_END}                                  {  if (curlyCount > 0) {
                                                    curlyCount--;
                                                    return GROOVY_EXPR_CODE;
                                                 } else {
                                                   curlyCount = 0;
                                                   yybegin(ATTR_VALUE_SQ_NOT_MAP);
                                                   return GEXPR_END;
                                                 }
                                               }
  [^}]                                         {  return GROOVY_EXPR_CODE; }
}

<ATTR_VALUE_END> {
  [^]                                         {  yybegin(ATTR_LIST); return GSP_ATTR_VALUE_END_DELIMITER; }
}

////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////  Injection start  /////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
<INJECTION_START>{

  // Java injections
  {JDECLAR_BEGIN}                           {  yybegin (IN_J_DECLAR);
                                               return JDECLAR_BEGIN; }

  {JEXPR_BEGIN}                             {  yybegin (IN_J_EXPR);
                                               return JEXPR_BEGIN; }

  {JDIRECT_BEGIN}                           {  yypushback(3) ;
                                               yybegin (IN_J_DIRECT); }

  {JSCRIPT_BEGIN}                           {  yybegin (IN_J_SCRIPT);
                                               return JSCRIPT_BEGIN; }

  // Groovy injections
  {GEXPR_BEGIN}                             {  curlyCount = 0;
                                               yybegin (IN_GEXPR);
                                               return GEXPR_BEGIN; }

  {GSCRIPT_BEGIN}                           {  yybegin (IN_GSCRIPT);
                                               return GSCRIPT_BEGIN; }

  {GDIRECT_BEGIN}                           {  yypushback(2);
                                               yybegin (IN_GDIRECT); }

  {GDECLAR_BEGIN}                           {  yybegin (IN_GDECLAR);
                                               return GDECLAR_BEGIN; }

}

////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////  Common scripts  //////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

<IN_J_SCRIPT>{
  {JSCRIPT_END}                             {  yybegin(YYINITIAL); return JSCRIPT_END; }
  [^]                                       {  return GROOVY_CODE; }
}

<IN_GSCRIPT>{
  [^]                                       {  return GROOVY_CODE; }
  {GSCRIPT_END}                             {  yybegin (YYINITIAL); return GSCRIPT_END; }
}

////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////  Expression injects  //////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

<IN_J_EXPR>{
  [^]                                       {  return GROOVY_EXPR_CODE; }
  {JSCRIPT_END}                             {  yybegin(YYINITIAL); return JEXPR_END; }
}


<IN_GEXPR>{
  "{"                                       {  curlyCount++; return GROOVY_EXPR_CODE; }
  {GEXPR_END}                               {  if (curlyCount > 0) {
                                                 curlyCount--;
                                                 return GROOVY_EXPR_CODE;
                                               } else {
                                                curlyCount = 0;
                                                yybegin(YYINITIAL);
                                                return GEXPR_END;
                                               }
                                            }
  [^}]                                      {  return GROOVY_EXPR_CODE; }
}

////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////  Declarations  ////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
<IN_J_DECLAR> {
  {JSCRIPT_END}                             {  yybegin(YYINITIAL); return JDECLAR_END; }
  [^]                                       {  return GROOVY_DECLARATION; }
}


<IN_GDECLAR>{
  {GDECLAR_END}                             {  yybegin(YYINITIAL); return GDECLAR_END; }
  [^]                                       {  return GROOVY_DECLARATION; }
}

////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////  Directives  //////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
<IN_J_DIRECT> {
  {JSCRIPT_END}                             {  yybegin(YYINITIAL); return GSP_DIRECTIVE; }
  [^]                                       {  return GSP_DIRECTIVE; }
}


<IN_GDIRECT>{
  {GDIRECT_END}                             {  yybegin(YYINITIAL); return GSP_DIRECTIVE; }
  [^}]                                      {  return GSP_DIRECTIVE; }
}





