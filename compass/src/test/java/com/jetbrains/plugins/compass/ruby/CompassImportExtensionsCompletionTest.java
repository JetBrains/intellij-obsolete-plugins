package com.jetbrains.plugins.compass.ruby;

import org.jetbrains.annotations.NotNull;
import org.junit.Test;

public class CompassImportExtensionsCompletionTest extends SassExtensionsBaseTest {
  @Test
  public void testCompassFiles_1() {
    myFixture.testCompletionVariants(getTestFileName(), "compass", "compass");
  }

  @Test
  public void testCompassFiles_2() {
    myFixture.testCompletionVariants(getTestFileName(), "css3", "css3");
  }

  @NotNull
  @Override
  protected String getTestDataRelativePath() {
    return "completion";
  }
}
