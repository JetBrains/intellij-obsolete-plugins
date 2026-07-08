package com.intellij.lang.puppet.intentions;

import com.intellij.lang.puppet.PuppetTestUtil;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import org.jetbrains.annotations.NotNull;

public class PuppetRegexpIntentionTest extends BasePlatformTestCase {

  @Override
  protected @NotNull String getTestDataPath() {
    return PuppetTestUtil.getTestDataPath() + "intentions/";
  }

  public void testRegexp() {
    myFixture.configureByFile("regexp.pp");

    int offsetWithRegexp = myFixture.getFile().getText().indexOf("fedora");
    myFixture.getEditor().getCaretModel().moveToOffset(offsetWithRegexp);

    assertNotNull(myFixture.findSingleIntention("Check RegExp"));
    assertNotNull(myFixture.findSingleIntention("Edit RegExp Fragment"));
  }
}
