// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.groovy.grails.gsp;

import com.intellij.openapi.project.Project;
import com.intellij.psi.codeStyle.CommonCodeStyleSettings;
import com.intellij.psi.formatter.xml.HtmlCodeStyleSettings;
import org.jetbrains.plugins.grails.fileType.GspFileType;
import org.jetbrains.plugins.groovy.lang.GroovyFormatterTestCase;
import org.jetbrains.plugins.groovy.util.TestUtils;

import java.util.List;

import static org.jetbrains.plugins.groovy.grails.GrailsTestUtil.getTestRootPath;

public class GspFormatterTest extends GroovyFormatterTestCase {
  @Override
  protected void setSettings(Project project) {
    super.setSettings(project);
    CommonCodeStyleSettings.IndentOptions gsp = myTempSettings.getIndentOptions(GspFileType.GSP_FILE_TYPE);
    
    gsp.INDENT_SIZE = 2;
    gsp.CONTINUATION_INDENT_SIZE = 4;
    gsp.TAB_SIZE = 2;

    myTempSettings.setDefaultRightMargin(2000);
  }

  public void doTest() {
    final List<String> data = TestUtils.readInput(getTestDataPath() + getTestName(true).replace('$', '/') + ".test");
    myFixture.configureByText(GspFileType.GSP_FILE_TYPE, data.get(0));
    checkFormatting(data.get(1));
  }
  
  public void testComments$schulz1() { doTest(); }
  public void testComments$ven1() { doTest(); }
  public void testGroovy$groovy() { doTest(); }
  public void testGroovy$plain1() { doTest(); }
  public void testGroovy$sim1() { doTest(); }
  public void testGsp$gsp1() { doTest(); }
  public void testGsp$gsp2() { doTest(); }
  public void testGsp$gsp3() { doTest(); }
  public void testGsp$gsp8() { doTest(); }
  public void testGsp$gsp9() { doTest(); }
  public void testGsp$EA29587() { doTest(); }
  public void _testHtml$error$giga_werle() { doTest(); }
  public void testHtml$error$nik1() { doTest(); }
  public void testHtml$error$show() { doTest(); }
  public void testHtml$error$werle1() { doTest(); }
  public void testHtml$error$werle30() { doTest(); }
  public void testHtml$inner$GRVY_1165_1() { doTest(); }
  public void testHtml$inner$GRVY_1165() { doTest(); }
  public void testHtml$inner$GRVY_876() { doTest(); }
  public void testHtml$inner$inner2() { doTest(); }
  public void testHtml$inner$inner3() { doTest(); }
  public void testHtml$megabug$GRVY_1046() { doTest(); }
  public void testHtml$megabug$megap2() { doTest(); }
  public void testHtml$megabug$mmm3() { doTest(); }
  public void testHtml$megabug$peter() { doTest(); }
  public void testHtml$megabug$peter2() { doTest(); }
  public void testHtml$megabug$peter3() {
    getHtmlSettings().HTML_DO_NOT_ALIGN_CHILDREN_OF_MIN_LINES = 1000;
    doTest();
  }
  public void testHtml$megabug$bigTest() {
    getHtmlSettings().HTML_DO_NOT_ALIGN_CHILDREN_OF_MIN_LINES = 1000;
    doTest();
  }
  public void testHtml$megabug$range1() { doTest(); }
  public void testHtml$nested$attr1() { doTest(); }
  public void testHtml$nested$gsp6() { doTest(); }
  public void testHtml$nested$gsp7() { doTest(); }
  public void testHtml$nested$peter7926() { doTest(); }
  public void testHtml$nested$peter_simple() { doTest(); }
  public void testHtml$nested$range() { doTest(); }
  public void testHtml$nested$range2() { doTest(); }
  public void testHtml$nested$range4() { doTest(); }
  public void testHtml$nested$range5() { doTest(); }
  public void testHtml$simple$GRVY_1146() { doTest(); }
  public void testHtml$simple$gsp4() { doTest(); }
  public void testHtml$simple$gsp8() { doTest(); }
  public void testHtml$simple$htm1() { doTest(); }
  public void testHtml$simple$htm2() { doTest(); }
  public void testHtml$simple$range3() { doTest(); }
  public void testHtml$simple$sim2() { doTest(); }
  public void testHtml$trash$bug1() { doTest(); }
  public void testHtml$trash$bug2() { doTest(); }
  public void testHtml$trash$bug3() { doTest(); }
  public void testHtml$trash$bug4() { doTest(); }
  public void testHtml$trash$bug5() { doTest(); }
  public void testHtml$trash$trash1() { doTest(); }
  public void testSpacing$groovy$gr1() { doTest(); }
  public void testSpacing$groovy$gr2() { doTest(); }
  public void testSpacing$htm3() { doTest(); }
  public void testSpacing$spac2() { doTest(); }
  public void testSpacing$spac3() { doTest(); }
  public void testSpacing$tags2() { doTest(); }
  public void testHyphenInTagName() { doTest(); }
  public void testHtml$simple$attributeSpaces() {
    getHtmlSettings().HTML_SPACE_AROUND_EQUALITY_IN_ATTRIBUTE = true;
    getHtmlSettings().HTML_SPACE_INSIDE_EMPTY_TAG = true;
    doTest();
  }

  public void testHtml$simple$tagNameSpaces() {
    getHtmlSettings().HTML_SPACE_AFTER_TAG_NAME = true;
    doTest();
  }

  @Override
  protected String getTestDataPath() {
    return getTestRootPath("/testdata/grails/formatter/");
  }

  public void testFormatWrapAlways() {
    getHtmlSettings().HTML_ATTRIBUTE_WRAP = CommonCodeStyleSettings.WRAP_ALWAYS;

    myFixture.configureByText("a.gsp", """
      <html>
      <head>
          <title>Title</title>
      </head>
      <body>
          <div>  <g:link action="aaa" controller="bbb" dir="ddd">The link</g:link></div>

              <a href="dsfsdfjsdlfsd"    title="asdasdfjsklf" class="asdajsd" style="clear:both;">Rrrrr</a>
              dfsdfs
      </body>
      </html>""");
    checkFormatting("""
                      <html>
                      <head>
                        <title>Title</title>
                      </head>

                      <body>
                      <div><g:link action="aaa"
                                   controller="bbb"
                                   dir="ddd">The link</g:link></div>

                      <a href="dsfsdfjsdlfsd"
                         title="asdasdfjsklf"
                         class="asdajsd"
                         style="clear:both;">Rrrrr</a>
                      dfsdfs
                      </body>
                      </html>""");
  }

  public void testIndentLongHtmlTags() {
    StringBuilder sb = new StringBuilder();
    sb.append("<div>\n");
    for (int i = 0; i < 300; i++) {
      sb.append("   a\n");
    }
    sb.append(" <span>qqq</span>\n");
    sb.append(" <g:span>qqq</g:span>\n");
    sb.append("</div>");

    myFixture.configureByText("a.gsp", sb.toString());

    checkFormatting(sb.toString().replaceAll("\n +", "\n  "));
  }

  public void testIndentLongGspTags() {
    StringBuilder sb = new StringBuilder();
    sb.append("<g:div>\n");
    for (int i = 0; i < 300; i++) {
      sb.append("   a\n");
    }
    sb.append(" <span>qqq</span>\n");
    sb.append(" <g:span>qqq</g:span>\n");
    sb.append("</g:div>");

    myFixture.configureByText("a.gsp", sb.toString());

    checkFormatting(sb.toString().replaceAll("\n +", "\n  "));
  }

  public void testInsertBeforeTag() {
    myFixture.configureByText("a.gsp", """
      <body>
      <span>sas</span>
      <div>
        asdasda
      </div>

      </body>""");

    checkFormatting("""
                      <body>
                      <span>sas</span>

                      <div>
                        asdasda
                      </div>

                      </body>""");
  }

  private HtmlCodeStyleSettings getHtmlSettings() {
    return myTempSettings.getCustomSettings(HtmlCodeStyleSettings.class);
  }
}
