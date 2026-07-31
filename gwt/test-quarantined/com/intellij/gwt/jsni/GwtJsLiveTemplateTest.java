package com.intellij.gwt.jsni;

import com.intellij.gwt.GwtTestCase;
import com.intellij.gwt.javascript.GwtJsTestUtil;
import com.intellij.lang.javascript.JSLiveTemplatesTestBase;

public class GwtJsLiveTemplateTest extends JSLiveTemplatesTestBase {
  public void testItar() {
    doTest("itar", "GwtJavaScript", "JavaScript");
  }

  @Override
  protected String getBasePath() {
    return "/jsni/liveTemplate/";
  }

  @Override
  protected String getTestDataPath() {
    return GwtTestCase.getGwtTestDataPath();
  }

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    GwtJsTestUtil.setUpGwtDialect();
  }
}
