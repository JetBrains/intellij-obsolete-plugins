package com.intellij.gwt.inspections;

import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.gwt.references.GwtCodeInsightTestCase;

import java.util.List;

public class GwtToCssClassReferencesInspectionTest extends GwtCodeInsightTestCase {

  public void testWithoutStyleTag() {
    doTest("WithoutStyleTag.ui.xml", "WithoutStyleTag.ui.xml", "WithStyleTag_after.ui.xml");
  }

  public void testWithStyleTag() {
    doTest("WithStyleTag.ui.xml", "WithStyleTag.ui.xml", "WithStyleTag_after.ui.xml");
  }

  public void testWithCollapsedStyleTag() {
    doTest("WithCollapsedStyleTag.ui.xml", "WithCollapsedStyleTag.ui.xml", "WithStyleTag_after.ui.xml");
  }

  public void testWithCssReferencedStyleTag() {
    doTest("WithCssReferencedStyleTag.ui.xml", "style.css", "style_after.css");
  }

  private void doTest(String fileName, String fileBefore, String fileAfter) {
    myCodeInsightFixture.enableInspections(new GwtToCssClassReferencesInspection());

    List<IntentionAction> quickFixes = myCodeInsightFixture.getAllQuickFixes(fileName);
    IntentionAction action = assertOneElement(quickFixes);
    myCodeInsightFixture.launchAction(action);
    if (!fileName.equals(fileBefore)) {
      myCodeInsightFixture.openFileInEditor(myCodeInsightFixture.findFileInTempDir(fileBefore));
    }
    myCodeInsightFixture.checkResultByFile(fileAfter);
  }

  @Override
  protected String getBaseDirectoryPath() {
    return "inspections/gwtToCssClassReferencesInspection";
  }
}
