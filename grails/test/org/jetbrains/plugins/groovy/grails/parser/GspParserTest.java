// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.groovy.grails.parser;

import org.jetbrains.plugins.grails.lang.gsp.GspLanguage;

import static org.jetbrains.plugins.groovy.grails.GrailsTestUtil.getTestRootPath;

public class GspParserTest extends GspParsingTestCase {
  public void testDir$dir1() { doTest(); }
  public void testInject$escaped1() { doTest(); }
  public void testInject$GRVY_943() { doTest(); }
  public void testSimple$bubug1() { doTest(); }
  public void testSimple$clos1() { doTest(); }
  public void testSimple$clos2() { doTest(); }
  public void testSimple$common() { doTest(); }
  public void testSimple$form1() { doTest(); }
  public void testSimple$megap1() { doTest(); }
  public void testSimple$mmm1() { doTest(); }
  public void testSimple$peter2() { doTest(); }
  public void testTags$act1() { doTest(); }
  public void testTags$act2() { doTest(); }
  public void testTags$gps9() { doTest(); }
  public void testTags$orph1() { doTest(); }
  public void testTags$tag3() { doTest(); }
  public void testTags$tagWithSlash() { doTest(); }
  public void testTags$uglyAttributeName() { doTest(); }
  public void testTags$unendedAttrList() { doTest(); }
  public void testHyphenInTagName() { doTest(); }

  private void doTest() {
    doTest(GspLanguage.INSTANCE);
  }

  @Override
  protected String getTestDataPath() {
    return getTestRootPath("/testdata/grails/parser/gsp/");
  }

}
