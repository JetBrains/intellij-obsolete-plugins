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
import org.jetbrains.plugins.grails.lang.gsp.lexer.*;

import static com.intellij.psi.xml.XmlTokenType.*;

%%

%class _GspDirectiveLexer
%implements FlexLexer, GspTokenTypesEx
%unicode
%public

%function advance
%type IElementType

////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
/////////////////////// User code //////////////////////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

%{ // User code


%}

////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
////////// Directive lexems ////////////////////////////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

mONE_NL = \r | \n | \r\n                                    // NewLines
mWS = " " | \t | \f | {mONE_NL}                             // Whitespaces

mLETTER = !(!([:jletter:] | "_") | "$")
mIDENT = {mLETTER} ({mLETTER} | [0-9])*

JDIRECT_BEGIN = "<%@"
JSCRIPT_END = "%>"
GDIRECT_BEGIN = "@{"
GDIRECT_END = "}"


////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
////////////////////  states ///////////////////////////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

%xstate ATTR_LIST
%xstate ATTR
%xstate ATTR_VALUE
%xstate ATTR_VALUE_DQ, ATTR_VALUE_SQ
%xstate BAD


%%
<YYINITIAL> {
  {JDIRECT_BEGIN}                              {  return JDIRECT_BEGIN; }
  {GDIRECT_BEGIN}                              {  return GDIRECT_BEGIN; }
  {JSCRIPT_END}                                {  yybegin(YYINITIAL); return JDIRECT_END; }
  {GDIRECT_END}                                {  yybegin(YYINITIAL); return GDIRECT_END; }
  {mWS}+                                       {  return XML_WHITE_SPACE; }
  {mIDENT}                                     {  yybegin(ATTR_LIST);
                                                  return XML_TAG_NAME; }
  [^]                                          {  return GSP_BAD_CHARACTER; }
}

<ATTR_LIST> {
  {mWS}+                                       {  return XML_WHITE_SPACE; }
  {mIDENT}                                     {  yybegin(ATTR);
                                                  return XML_NAME; }
  {JSCRIPT_END}                                {  yybegin(YYINITIAL); return JDIRECT_END; }
  {GDIRECT_END}                                {  yybegin(YYINITIAL); return GDIRECT_END; }
  [^]                                          {  return GSP_BAD_CHARACTER; }
}

<ATTR> {
  {mWS}+                                       {  return XML_WHITE_SPACE; }
  "="                                          {  yybegin(ATTR_VALUE); return XML_EQ; }
  {JSCRIPT_END}                                {  yybegin(YYINITIAL); return JDIRECT_END; }
  {GDIRECT_END}                                {  yybegin(YYINITIAL); return GDIRECT_END; }
  [^]                                          {  yybegin(ATTR_LIST); return GSP_BAD_CHARACTER; }
}

<ATTR_VALUE> {
  {mWS}+                                       {  return XML_WHITE_SPACE; }
  \"                                           {  yybegin(ATTR_VALUE_DQ); return XML_ATTRIBUTE_VALUE_START_DELIMITER; }
  \'                                           {  yybegin(ATTR_VALUE_SQ); return XML_ATTRIBUTE_VALUE_START_DELIMITER; }
  [^]                                          {  yybegin(ATTR_LIST); return GSP_BAD_CHARACTER; }
}

<ATTR_VALUE_DQ> {
  [^\"]+ \"                                    {  yypushback(1); return XML_ATTRIBUTE_VALUE_TOKEN; }
  \"                                           {  yybegin(ATTR_LIST); return XML_ATTRIBUTE_VALUE_END_DELIMITER; }
  [^\"] | {mONE_NL}                            {  yybegin(ATTR_LIST); return GSP_BAD_CHARACTER; }
}

<ATTR_VALUE_SQ> {
  [^\']+ \'                                    {  yypushback(1); return XML_ATTRIBUTE_VALUE_TOKEN; }
  \'                                           {  yybegin(ATTR_LIST); return XML_ATTRIBUTE_VALUE_END_DELIMITER; }
  [^\'] | {mONE_NL}                            {  yybegin(ATTR_LIST); return GSP_BAD_CHARACTER; }
}

<BAD> {
  [^]                                          {  return GSP_BAD_CHARACTER; }
}
