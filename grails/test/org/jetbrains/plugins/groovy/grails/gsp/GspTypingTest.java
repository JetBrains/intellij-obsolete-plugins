// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.groovy.grails.gsp;

import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase;
import org.jetbrains.plugins.grails.fileType.GspFileType;

public class GspTypingTest extends LightJavaCodeInsightFixtureTestCase {
  private void doTest(String text, String type, String result) {
    myFixture.configureByText(GspFileType.GSP_FILE_TYPE, text);
    for (int i = 0; i < type.length(); i++) {
      myFixture.type(type.charAt(i));
    }
    myFixture.checkResult(result);
  }

  public void testT1() {
    doTest("%{<caret>", "\n", "%{\n    }%");
  }

  public void testT3() {
    doTest("<%<caret>", "\n", "<%\n    <caret>\n%>");
  }

  public void testT4() {
    doTest("$<caret>", "{", "${<caret>}");
  }

  public void testT5() {
    doTest("<%<caret>", "!", "<%! <caret> %>");
  }

  public void testT6() {
    doTest("<%<caret>", "@", "<%@ <caret> %>");
  }

  public void testT7() {
    doTest("<%<caret>", "=", "<%= <caret> %>");
  }

  public void testT8() {
    doTest("<%<caret> x %>", "=", "<%= x %>");
  }

  public void testT9() {
    doTest("<%<caret> x <%= eee %>", "=", "<%= <caret> %> x <%= eee %>");
  }

  public void testT10() {
    doTest("<%<caret> x <% eee %>", "=", "<%= <caret> %> x <% eee %>");
  }

  public void testT11() {
    doTest("<%<caret> x %{ eee }%", "=", "<%= <caret> %> x %{ eee }%");
  }

  public void testT12() {
    doTest("%{<caret> %> }%", "\n", "%{\n     %> }%");
  }

  public void testT13() {
    doTest("<%<caret> }% %>", "\n", "<%\n     }% %>");
  }

  public void testBracketQCompletion() {
    doTest("<g:each var=<caret> />", "\"", "<g:each var=\"<caret>\" />");
  }

  public void testBracketSCompletion() {
    doTest("<g:each var=<caret> />", "'", "<g:each var='<caret>' />");
  }

  public void testBracketGroovyCompletion() {
    doTest("${ <caret> }", "'", "${ '<caret>' }");
  }

  public void testBracketInGspAttribute() {
    doTest("<g:link action=\"a<caret>a\" />", "${", "<g:link action=\"a${<caret>}a\" />");
  }

  public void testBracketInHtmlAttribute() {
    doTest("<a href=\"a<caret>a\" />", "${", "<a href=\"a${<caret>}a\" />");
  }

  public void testBracketInEmptyGspAttribute() {
    doTest("<g:link action=\"<caret>\" />", "${", "<g:link action=\"${<caret>}\" />");
  }

  public void testBracketInEmptyHtmlAttribute() {
    doTest("<a href=\"<caret>\" />", "${", "<a href=\"${<caret>}\" />");
  }

  public void testRightBracketInGspAttribute() {
    doTest("<g:link action=\"a<caret>a\" />", "${", "<g:link action=\"a${}a\" />");
  }

  public void testRightBracketInHtmlAttribute() {
    doTest("<a href=\"a<caret>a\" />", "${", "<a href=\"a${}a\" />");
  }

  public void testRightBracketInEmptyGspAttribute() {
    doTest("<g:link action=\"<caret>\" />", "${}", "<g:link action=\"${}<caret>\" />");
  }

  public void testRightBracketInEmptyHtmlAttribute() {
    doTest("<a href=\"<caret>\" />", "${}", "<a href=\"${}<caret>\" />");
  }

  public void testHtmlTagEnd() {
    doTest("<a href=\"\" <caret> <% 1 %>", "/", "<a href=\"\" /> <% 1 %>");
  }

  public void testGspTagEnd1() {
    doTest("<g:link action=\"aaa\" <caret> <% 1 %>", "/", "<g:link action=\"aaa\" /> <% 1 %>");
  }

  public void testGspTagEnd2() {
    doTest("<g:link action=\"aaa\" <caret> ${1}", "/", "<g:link action=\"aaa\" /> ${1}");
  }

  public void testTypePairedBracket_IDEA66500() {
    doTest("<g:if test=\"${foo<caret>}\">", "[", "<g:if test=\"${foo[<caret>]}\">");
  }

  public void testCompletionSlash1() {
    doTest("<g:link<caret>", "/", "<g:link/><caret>");
  }

  public void testCompletionSlash1a() {
    doTest("<g:link<caret> ", "/>", "<g:link/><caret> ");
  }

  public void testCompletionSlash2() {
    doTest("<g:lin<caret>k", "/", "<g:lin/<caret>k");
  }

  public void testCompletionSlash3() {
    doTest("<link<caret>", "/", "<link/><caret>");
  }

  public void testCompletionSlash3a() {
    doTest("<link<caret> ", "/>", "<link/><caret> ");
  }

  public void testCompletionSlash4() {
    doTest("<lin<caret>k", "/", "<lin/<caret>k");
  }

  public void testCompletionSlash5() {
    doTest("<tmpl:ttt<caret>t", "/", "<tmpl:ttt/<caret>t");
  }

  public void testCompletionSlash6() {
    doTest("<tmpl:<caret>", "/", "<tmpl:/<caret>");
  }

  public void testCompletionSlash7() {
    doTest("<tmpl:ttt<caret>", "/", "<tmpl:ttt/");
  }

  public void testCompletionAfterSlashInName() {
    doTest("<body> <tmpl:ttt/<caret> </body>", ">", "<body> <tmpl:ttt/> </body>");
  }

  public void testCompletionAfterSlashInName2() {
    doTest("<g:link/<caret>", ">", "<g:link/>");
  }

  public void testCompletionAfterSlashInAttr() {
    doTest("<body> <tmpl:ttt zzz='zzz'/<caret> </body>", ">", "<body> <tmpl:ttt zzz='zzz'/> </body>");
  }

  public void testSlashInTag1() {
    doTest("<body> <g:link<caret>></g:link> </body>", "/", "<body> <g:link/<caret>> </body>");
  }

  public void testSlashInTag2() {
    doTest("<body> <g:link/<caret>> </body>", ">", "<body> <g:link/><caret> </body>");
  }

  public void testSlashInTag3() {
    doTest("<body> <g:link<caret>> </body>", "/", "<body> <g:link/<caret>> </body>");
  }

  public void testSlashInTag4() {
    doTest("<body> <g:link <caret><g:link/> </body>", "/", "<body> <g:link /><g:link/> </body>");
  }

  public void testSlashInTag5() {
    doTest("<body> <g:link a='1' <caret><g:link/> </body>", "/", "<body> <g:link a='1' /><g:link/> </body>");
  }

  public void testSquareBracketInsert1() {
    doTest("<g:link a='<caret>' />", "[", "<g:link a='[<caret>]' />");
  }

  public void testSquareBracketInsert2() {
    doTest("<g:link a=\"<caret>\" />", "[", "<g:link a=\"[<caret>]\" />");
  }

  public void testSquareBracketInsert3() {
    doTest("<g:link a=\"<caret>\" />", "[]", "<g:link a=\"[]<caret>\" />");
  }

  public void testSquareBracketInsert4() {
    doTest("<g:link a='<caret>' />", "[a:1]", "<g:link a='[a:1]<caret>' />");
  }

  public void testSquareBracketInsert5() {
    doTest("<g:link a=' <caret>' />", "[", "<g:link a=' [<caret>' />");
  }
}
