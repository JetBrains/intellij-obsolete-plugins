/*
 * Copyright (c) 2000-2005 by JetBrains s.r.o. All Rights Reserved.
 * Use is subject to license terms.
 */
package com.intellij.frameworks.play;

import com.intellij.lexer.Lexer;
import com.intellij.play.language.lexer.PlayLexer;
import com.intellij.testFramework.LexerTestCase;
import org.jetbrains.annotations.NonNls;

public class PlayLexerTest extends LexerTestCase {
  @Override
  protected Lexer createLexer() {
    return PlayLexer.createLexer();
  }

  @Override
  protected String getDirPath() {
    return "src/test/testData/lexer/";
  }

  public void testExpressions() {
    doTest("Connected user is ${user}.");
  }

  public void testActions() {
    doTest("<a href=\"@{leave(user)}\">Leave the chat room</a>");
  }

  public void testActions2() {
    doTest("aaa @{ {a+b} } bbb");
  }

  public void testActions3() {
    doTest("aaa @{ {a+b } bbb");
  }

  public void testComments() {
    doTest(" html text *{ comment }* bbb");
  }

  public void testComments2() {
    doTest(" html text *{ %{foo}% &{action} co*{mm}*ent }* bbb>");
  }

  public void testComments3() {
    doTest(" html text *{comment");
  }
  public void testComments5() {
    doTest("*{some dummy template *{to}* check comments }*");
  }

  public void testComments6() {
    doTest("*{*{*{*{}*}*}*}*");
  }

  public void testTags() {
    doTest("<div id=\"thread\">#{list events} do it #{/list} </div>");
  }

  public void testTagWithActions() {
    doTest("<div id=\"thread\">#{form @get(events)} do it #{/form} </div>");
  }

  public void testSimpleTag() {
    doTest("header #{authenticityToken /} footer ");
  }

  public void testTogether() {
    doTest("#{extends aaa:'main.html', @get(events),'index.html'  /}");
  }

  public void testTogether2() {
    doTest("var waitMessages = #{jsAction @waitMessages(':lastReceived') /}");
  }

  public void testTogether3() {
    doTest("#{list items:forums, as:'forum'}");
  }

  public void testTogether4() {
    doTest("#{error 'name' /}");
  }

  public void testActionAttribute() {
    doTest("#{myTag  action:@MyController.action() /}");
  }

  public void testActionAttribute2() {
    doTest("#{myTag  action:@MyController.action(),'' /}");
  }

  public void testActionAttribute3() {
    doTest("#{myTag aaa:12, action:@MyController.action(), encr:'aaa' /}");
  }

  public void testTogether5() {
    doTest("""
             <p>
             \t\t<label>Display name</label>
             \t\t<input type="text" name="name" value="${flash.name}" id="name" />
             \t\t<span class="error">#{error 'name' /}
             \t</p>
             \t<p>""");
  }

  public void testTogether6() {
    doTest("#{error 'name /}");
  }

  public void testTogether7() {
    doTest("#{error 'name' khkh");
  }

  public void testTogether8() {
    doTest("template text #{list events} foo  #{/list}");
  }

  public void testTagWithChildren() {
    doTest("#{form }#{child2/}#{/form}");
  }

  public void testIncorrectTagStart2() {
    doTest("#{form expr#{newtag exp}sss #{/newtag} booo ");
  }

  public void testIncorrectTagStart() {
    doTest("#{form} #{ #{/form}");
  }

  public void testStringExpressions() {
    doTest("#{tag aa:'aa', bb:a /} aaa");
  }

  public void testEmptyEl() {
    doTest("${}");
  }



  public void testPagination() {
    doTest("#{pagination page:page ?: 1, size:forum.topicsCount /}}");
  }

  public void testArrays() {
    doTest("#{crud.table fields:['name', 'email'] /}");
  }

  public void testBrackets1() {
    doTest("#{c attr:'{[]}', ['[]'] /}");
  }

  public void testBrackets2() {
    doTest("#{c new String[]{'aa', 'bbb',''}/}");
  }

  public void testBrackets3() {
    doTest("#{c bbb:[]() /}");
  }

  public void testBrackets4() {
    doTest("#{c bbb:[([''])]('[]') /}");
  }

  public void testBrackets5() {
    doTest("#{c bbb:[([''])]('[]'), aaa:[() /}");
  }

  public void testCrud() {
    doTest("#{crud.form fields:superadmin ? ['title', 'place'] : ['detail', ] /}");
  }

  public void testSetTag() {
    doTest("#{foo a:,/}");
  }

  public void testSetTag2() {
    doTest("#{foo :/}");
  }

  public void testInvalidExtendsTag() {
    doTest("#{extends ");
  }

  public void testInvalidExtendsTag2() {
    doTest("#{extends /");
  }

  public void testInvalidExtendsTag3() {
    doTest("#{extends '");
  }

  public void testInvalidExtendsTag4() {
    doTest("#{extends 'aaaa'");
  }

  public void testMessages() {
    doTest("foo &{'extends' } bar");
  }

  public void testMessages2() {
    doTest("foo &{'extends', a==3} bar");
  }

  public void testMessages3() {
    doTest("foo &{'extends',} bar");
  }

  public void testMessages4() {
    doTest("foo &{'extends', {a+b},'' } bar");
  }

  public void testMessages5() {
    doTest("foo &{'extends', {a+b}} bar");
  }

  public void testMessages6() {
    doTest("foo &{'extends',,, bar");
  }

  public void testMessages7() {
    doTest("foo &{'a,b', } bar");
  }

  public void testMessages8() {
    doTest("foo &{'a,b}");
  }

  public void testMessages9() {
    doTest("&{}");
  }

  public void testIncorrectTogether() {
    doTest("#{/if  #{list  ");
  }
  public void testIncorrectTogether2() {
    doTest("#{/if aaa}  #{list  ");
  }

  public void testIncorrectTogether3() {
    doTest("#{form @addBar()");
  }

  public void testPairBraces_IDEA_79778() {
    doTest("#{a @cashWithdraw().add(\"email\", m.emailAddress).add(\"hash\", m.emailAddress.sign()) }${m.emailAddress}#{/a} ${balances[m].amount}");
  }

  public void testPairBraces2() {
      doTest("#{a @cashWithdraw().add().add(())}${@m.emailAddress(aa())}#{/a} ${balances[m].amount}");
  }

  public void testPercents() {
      doTest("how many percent? that many: ${value}% xxxx this is an error ");
  }

  public void testComments4() {
      doTest("#{foo}*{text}*");
  }

  public void testQuotesWithComma() {
      doTest("#{foo 'aa,bb' /}");
  }

  public void testDoubleQuotesWithComma() {
      doTest("#{foo \"aa,bb\" /}");
  }


  @Override
  protected void doTest(@NonNls String text) {
    super.doTest(text);
    checkCorrectRestart(text);
  }
}
