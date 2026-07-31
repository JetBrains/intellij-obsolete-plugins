package com.intellij.gwt.uiBinder;

import com.intellij.gwt.references.GwtCodeInsightTestCase;

import java.util.List;

public class GwtUiXmlCompletionTest extends GwtCodeInsightTestCase {

  public void testEmpty() {
    doTest("urn:import:",
           "urn:import:com.google.gwt.user.client.ui",
           "urn:ui:com.google.gwt.uibinder");
  }

  public void testUrn() {
    doTest("urn:import:",
           "urn:import:com.google.gwt.user.client.ui",
           "urn:ui:com.google.gwt.uibinder");
  }

  public void testUrnImport() {
    doTest("urn:import:META-INF",
           "urn:import:com",
           "urn:import:java",
           "urn:import:javax",
           "urn:import:org",
           "urn:import:pkg");
  }

  public void testJava() {
    doTest("urn:import:java",
           "urn:import:javax",
           "urn:import:java.awt",
           "urn:import:java.beans",
           "urn:import:java.io",
           "urn:import:java.lang",
           "urn:import:java.net",
           "urn:import:java.rmi",
           "urn:import:java.security",
           "urn:import:java.sql",
           "urn:import:java.util");
  }

  private void doTest(String... expectedVariants) {
    List<String> actualVariants = myCodeInsightFixture.getCompletionVariants(getTestName(false) + ".ui.xml");
    assertContainsElements(actualVariants, expectedVariants);
  }

  @Override
  protected String getBaseDirectoryPath() {
    return "uiBinder/completion";
  }
}
