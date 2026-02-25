// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.groovy.grails;

import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase;
import org.jetbrains.plugins.grails.fileType.GspFileType;
import org.jetbrains.plugins.groovy.util.TestUtils;

import java.util.List;

import static org.jetbrains.plugins.groovy.grails.GrailsTestUtil.getTestRootPath;

public class GrEditorActionTest extends LightJavaCodeInsightFixtureTestCase {

  private void doTest() {
    final List<String> data = TestUtils.readInput(getTestDataPath() + getTestName(true) + ".test");

    myFixture.configureByText(GspFileType.GSP_FILE_TYPE, "");

    final String fileText = data.get(0);

    for (int i = 0; i < fileText.length(); i++) {
      final char charTyped = fileText.charAt(i);
      myFixture.type(charTyped);
    }
    myFixture.checkResult(data.get(1));
  }

  public void testDir() { doTest(); }
  public void testSimple_type() { doTest(); }

  @Override
  protected String getTestDataPath() {
    return getTestRootPath("/testdata/grails/enterAction/");
  }
}
