package com.intellij.gwt.intentions;

import com.intellij.gwt.uiBinder.CreateUiFieldIntention;

public class CreateUiFieldIntentionTest extends GwtIntentionTestBase {

  public void testNoFields() {
    doTest("MyComponentWithoutFields.ui.xml", "MyComponentWithoutFields.java", "MyComponentWithoutFields_after.java", new CreateUiFieldIntention());
  }

  public void testOneField() {
    doTest("MyComponentWithFirstField.ui.xml", "MyComponentWithFirstField.java", "MyComponentWithFirstField_after.java", new CreateUiFieldIntention());
  }

  public void testFieldAlreadyExist() {
    doTest("MyComponentWithBothFields.ui.xml", "MyComponentWithBothFields.java", "MyComponentWithBothFields.java", new CreateUiFieldIntention());
  }

  @Override
  protected String getBaseDirectoryPath() {
    return "intentions/createUiField";
  }
}
