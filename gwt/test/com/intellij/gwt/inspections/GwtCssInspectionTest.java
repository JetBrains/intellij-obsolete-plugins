package com.intellij.gwt.inspections;

import com.intellij.gwt.sdk.impl.GwtVersionImpl;
import com.intellij.psi.css.inspections.CssUnusedSymbolInspection;

public class GwtCssInspectionTest extends GwtInspectionsTestCase {
  public void testCssUsedFromGwt() {
    doTest(new CssUnusedSymbolInspection());
  }

  @Override
  protected GwtVersionImpl getVersion() {
    return null;
  }
}
