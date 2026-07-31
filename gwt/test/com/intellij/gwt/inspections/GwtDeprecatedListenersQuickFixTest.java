package com.intellij.gwt.inspections;

import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.gwt.GwtTestCase;
import com.intellij.gwt.inspections.eventListeners.GwtDeprecatedEventListenersInspection;
import com.intellij.gwt.sdk.impl.GwtVersionImpl;
import com.intellij.openapi.application.WriteAction;
import com.intellij.testFramework.builders.JavaModuleFixtureBuilder;
import com.intellij.testFramework.fixtures.JavaCodeInsightFixtureTestCase;
import com.intellij.util.containers.ContainerUtil;

import java.util.List;

public class GwtDeprecatedListenersQuickFixTest extends JavaCodeInsightFixtureTestCase {
  public void testAddAnonymousListener() { doTest(); }
  public void testListenerAssignedToLocal() { doTest(); }
  public void testListenerAssignedToField() { doTest(); }
  public void testListenerLocalInitialized() { doTest(); }
  public void testListenerFieldInitialized() { doTest(); }
  public void testAnonymousListenerInButtonConstructor() { doTest(); }
  public void testScrollListener() { doTest(); }
  public void testMouseWheelInnerClass() { doTest(); }
  public void testFocusListenerInnerClass() { doTest(); }
  public void testMouseDownListener() { doTest(); }
  public void testKeyPressListener() { doTest(); }
  public void testFocusListenerOnLostFocus() { doTest(); }
  public void testNotReplaceIfHasGetter() { doTest(); }
  public void testNotReplaceIfParameterIsUsed() { doTest(); }
  public void testNotReplaceInPublicField() { doTest(); }
  public void testNotReplacePassedToMethod() { doTest(); }
  public void testNotReplaceIfDoesntHaveMethods() { doTest(); }
  public void testNotReplaceIfTwoMethods() { doTest(); }

  private void doTest() {
    WriteAction.runAndWait(() -> {
      final GwtVersionImpl version = GwtVersionImpl.VERSION_1_6;
      GwtTestCase.addGwtFacet(myFixture.getModule(), version);
      GwtTestCase.addGwtLibrary(myFixture.getModule(), version);
    });

    myFixture.enableInspections(new GwtDeprecatedEventListenersInspection());

    final String testName = getTestName(true);
    boolean shouldFixBeAvailable = !testName.startsWith("not");
    final String fileName = shouldFixBeAvailable ? testName + "_before.java" : testName + ".java";
    final List<IntentionAction> intentions = myFixture.getAvailableIntentions(fileName);

    final List<IntentionAction> filtered = ContainerUtil.findAll(intentions, intentionAction -> {
      final String text = intentionAction.getText();
      return text.startsWith("Replace '") && text.contains("' with '");
    });

    if (shouldFixBeAvailable) {
      myFixture.launchAction(assertOneElement(filtered));
      myFixture.checkResultByFile(testName + "_after.java");
    }
    else {
      assertEmpty(filtered);
    }
  }

  @Override
  protected void tuneFixture(JavaModuleFixtureBuilder moduleBuilder) {
    moduleBuilder.setMockJdkLevel(JavaModuleFixtureBuilder.MockJdkLevel.jdk15);
  }

  @Override
  protected String getTestDataPath() {
    return GwtTestCase.getGwtTestDataPath() + "quickFixes/deprecatedEventListeners/";
  }
}
