package com.intellij.gwt.injected;

import com.intellij.codeInsight.unwrap.UnwrapTestCase;

public class GwtJsUnwrapTest extends UnwrapTestCase {
  public void testIf() {
    assertUnwrapped("""
                      if (1>2) {
                          <caret>alert('!!!');
                      }""",
                    "alert('!!!');");
  }

  public void testIfWithCodeBeforeAndAfter() {
    assertUnwrapped("""
                      var i = 1;
                      if (1>2) {
                          <caret>alert('!!!');
                      }
                      i++;""",
                    """
                      var i = 1;
                      alert('!!!');
                      i++;""");
  }

  @Override
  protected String createCode(String codeBefore) {
    return "public class A {\n" +
           "    public native void m() /*-{\n" +
           indentTwice(codeBefore) +
           "    }-*/;\n" +
           "}";
  }
}
