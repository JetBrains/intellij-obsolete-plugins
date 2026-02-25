// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.groovy.grails.parser;

import org.jetbrains.plugins.groovy.GroovyFileType;

import static org.jetbrains.plugins.groovy.grails.GrailsTestUtil.getTestRootPath;

public class GspGroovyParserTest extends GspParsingTestCase {

  public void testComments$com() { doTest(); }
  public void testComments$comm1() { doTest(); }
  public void testComments$commm2() { doTest(); }
  public void testControl$common() { doTest(); }
  public void testControl$for1() { doTest(); }
  public void testControl$for2() { doTest(); }
  public void testControl$if1() { doTest(); }
  public void testControl$if2() { doTest(); }
  public void testControl$swit1() { doTest(); }
  public void testControl$swit2() { doTest(); }
  public void testControl$foreach1() { doTest(); }
  public void testCustom$tag1() { doTest(); }
  public void testCustom$tag2() { doTest(); }
  public void testCustom$tag3() { doTest(); }
  public void testDeclarations$dec1() { doTest(); }
  public void testDeclarations$dec2() { doTest(); }
  public void testDeclarations$dec3() { doTest(); }
  public void testDeclarations$dec4() { doTest(); }
  public void testDirect$dir1() { doTest(); }
  public void testErrors$err1() { doTest(); }
  public void testErrors$err2() { doTest(); }
  public void testErrors$err3() { doTest(); }
  public void testErrors$err4() { doTest(); }
  public void testSimple$clos1() { doTest(); }
  public void testSimple$inj1() { doTest(); }
  public void testSimple$inj2() { doTest(); }
  public void testSimple$nl() { doTest(); }
  public void testSimple$stat1() { doTest(); }

  private void doTest() {
    doTest(GroovyFileType.GROOVY_FILE_TYPE.getLanguage());
  }

  @Override
  protected String getTestDataPath() {
    return getTestRootPath("/testdata/grails/parser/gspGroovy/");
  }

}
