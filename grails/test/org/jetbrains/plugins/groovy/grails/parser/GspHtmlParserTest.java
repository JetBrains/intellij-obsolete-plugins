// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.groovy.grails.parser;

import com.intellij.lang.html.HTMLLanguage;

import static org.jetbrains.plugins.groovy.grails.GrailsTestUtil.getTestRootPath;

public class GspHtmlParserTest extends GspParsingTestCase {

  public void testAlone() { doTest(); }
  public void testCommon() { doTest(); }
  public void testHtml1() { doTest(); }
  public void testInject1() { doTest(); }
  public void testMmm2() { doTest(); }
  public void testMmm5() { doTest(); }
  public void testPeter1() { doTest(); }
  public void testPeter2() { doTest(); }
  public void testRange_parse() { doTest(); }
  public void testWerle() { doTest(); }
  public void testWerle28() { doTest(); }
  public void testWerle29() { doTest(); }
  public void testAfterWhitespace() { doTest(); }

  private void doTest() {
    doTest(HTMLLanguage.INSTANCE);
  }

  @Override
  protected String getTestDataPath() {
    return getTestRootPath("/testdata/grails/parser/gspHtml/");
  }

}