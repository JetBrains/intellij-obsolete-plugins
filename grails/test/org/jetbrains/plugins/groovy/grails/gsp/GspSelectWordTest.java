// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.groovy.grails.gsp;

import com.intellij.openapi.actionSystem.IdeActions;
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase;

public class GspSelectWordTest extends LightJavaCodeInsightFixtureTestCase {
  private void doTest(String src, String... variants) {
    myFixture.configureByText("a.gsp", src);

    for (String v : variants) {
      myFixture.performEditorAction(IdeActions.ACTION_EDITOR_SELECT_WORD_AT_CARET);
      myFixture.checkResult(v);
    }


    for (int i = variants.length - 2; i >= 0; i--) {
      myFixture.performEditorAction("EditorUnSelectWord");
      myFixture.checkResult(variants[i]);
    }
  }

  public void testFromGrailsTagName() {
    doTest("<div>aaa<g:li<caret>nk>Text</g:link>aaa</div>", "<div>aaa<g:<selection>link</selection>>Text</g:link>aaa</div>",
           "<div>aaa<<selection>g:link</selection>>Text</g:link>aaa</div>", "<div>aaa<selection><g:link></selection>Text</g:link>aaa</div>",
           "<div>aaa<selection><g:link>Text</g:link></selection>aaa</div>", "<div><selection>aaa<g:link>Text</g:link>aaa</selection></div>",
           "<selection><div>aaa<g:link>Text</g:link>aaa</div></selection>");
  }

  public void testFromText() {
    doTest("<div>aaa<g:link>Te<caret>xt</g:link>aaa</div>", "<div>aaa<g:link><selection>Text</selection></g:link>aaa</div>",
           "<div>aaa<selection><g:link>Text</g:link></selection>aaa</div>", "<div><selection>aaa<g:link>Text</g:link>aaa</selection></div>",
           "<selection><div>aaa<g:link>Text</g:link>aaa</div></selection>");
  }

  public void testFromAttrName() {
    doTest("<div>aaa<g:link absolu<caret>te=''>Text</g:link>aaa</div>",
           "<div>aaa<g:link <selection>absolute</selection>=''>Text</g:link>aaa</div>",
           "<div>aaa<g:link <selection>absolute=''</selection>>Text</g:link>aaa</div>",
           "<div>aaa<selection><g:link absolute=''></selection>Text</g:link>aaa</div>",
           "<div>aaa<selection><g:link absolute=''>Text</g:link></selection>aaa</div>",
           "<div><selection>aaa<g:link absolute=''>Text</g:link>aaa</selection></div>");
  }

  public void testFromGroovyInjection() {
    doTest("<div>aaa${'s<caret>ss'}aaa</div>", "<div>aaa${'<selection>sss</selection>'}aaa</div>",
           "<div>aaa${<selection>'sss'</selection>}aaa</div>", "<div>aaa<selection>${'sss'}</selection>aaa</div>",
           "<div><selection>aaa${'sss'}aaa</selection></div>", "<selection><div>aaa${'sss'}aaa</div></selection>");
  }

  public void testFromGroovyInjectionInHtmlAttribute() {
    doTest("<div>aaa<div aaa=\"${s<caret>ss}\"></div>aaa</div>", "<div>aaa<div aaa=\"${<selection>sss</selection>}\"></div>aaa</div>",
           "<div>aaa<div aaa=\"<selection>${sss}</selection>\"></div>aaa</div>",
           "<div>aaa<div aaa=<selection>\"${sss}\"</selection>></div>aaa</div>",
           "<div>aaa<div <selection>aaa=\"${sss}\"</selection>></div>aaa</div>");
  }

  public void testFromGroovyInjectionInGspAttribute() {
    doTest("<div>aaa<g:div aaa=\"${s<caret>ss}\"></g:div>aaa</div>",
           "<div>aaa<g:div aaa=\"${<selection>sss</selection>}\"></g:div>aaa</div>",
           "<div>aaa<g:div aaa=\"<selection>${sss}</selection>\"></g:div>aaa</div>",
           "<div>aaa<g:div aaa=<selection>\"${sss}\"</selection>></g:div>aaa</div>",
           "<div>aaa<g:div <selection>aaa=\"${sss}\"</selection>></g:div>aaa</div>");
  }

  public void testSpacesBeforeTag() {
    doTest("""
             
             <table>
              <caret> <g:each in="">
                  aaa
               </g:each>
             </table>
             """, """
             
             <table>
             <selection>  </selection><g:each in="">
                  aaa
               </g:each>
             </table>
             """, """
             
             <table>
             <selection>  <g:each in="">
             </selection>     aaa
               </g:each>
             </table>
             """, """
             
             <table>
             <selection>  <g:each in="">
                  aaa
               </g:each>
             </selection></table>
             """);
  }
}
