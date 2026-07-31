package com.intellij.gwt.css;

import com.intellij.gwt.references.GwtCodeInsightTestCase;
import com.intellij.psi.css.inspections.CssUnknownPropertyInspection;
import com.intellij.psi.css.inspections.invalid.CssInvalidPropertyValueInspection;

public class GwtCssInspectionTest extends GwtCodeInsightTestCase {
  public void testIgnorePropertyValuesWithVariables() {
    myCodeInsightFixture.enableInspections(CssInvalidPropertyValueInspection.class);
    myCodeInsightFixture.testHighlighting(getTestName(true) + ".css");
  }

  public void testOuterDefinition() {
    myCodeInsightFixture.enableInspections(CssInvalidPropertyValueInspection.class);
    myCodeInsightFixture.testHighlighting("MyComponent.ui.xml");
  }

  public void testIgnoreGwtCssContext() {
    // CssElementDescriptorProvider.getCssContextType) should fallback to Scss-specific provider
    myCodeInsightFixture.enableInspections(CssUnknownPropertyInspection.class);
    myCodeInsightFixture.testHighlighting(getTestName(true) + ".scss");
  } 

  @Override
  protected String getBaseDirectoryPath() {
    return "css/inspection";
  }
}
