/*
 * Copyright (c) 2000-2005 by JetBrains s.r.o. All Rights Reserved.
 * Use is subject to license terms.
 */
package com.intellij.frameworks.play;

import com.intellij.lexer.Lexer;
import com.intellij.play.language.lexer.PlayScriptLexer;
import com.intellij.testFramework.LexerTestCase;
import org.jetbrains.annotations.NonNls;

public class PlayScriptLexerTest extends LexerTestCase {
  @Override
  protected Lexer createLexer() {
    return new PlayScriptLexer();
  }

  @Override
  protected String getDirPath() {
    return "plugins/frameworks/play/tests/testData/lexer/";
  }

  public void testScripts() {
    doTest("<script type=\"text/html\" id=\"message_tmpl\">%{ if(event.type == 'message') { }% more html %{ { user(event) } }% bbbb ");
  }
  public void testScripts2() {
    doTest("%{ if(event.type == 'message') { user(event) } }%");
  }

  public void testEmptyScript() {
    doTest("%{}%");
  }

  public void testGroovyInScript() {
    doTest("foo %{ if a==2 }% boo");
  }

  public void testGroovyInScript2() {
    doTest("foo %{ if a==2 }}% boo");
  }

  public void testGroovyInScript3() {
    doTest("aaa %{ 1=1");
  }

  public void testGroovyInScript4() {
    doTest("%{x=1}%%{a=2}%");
  }

 public void testGroovyInScript5() {
    doTest("%{x=1}%{a=2}%");
  }

  @Override
  protected void doTest(@NonNls String text) {
    super.doTest(text);
    checkCorrectRestart(text);
  }
}
