package com.jetbrains.plugins.compass.ruby;

import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.scss.psi.SassScssStylesheetFile;
import org.junit.Test;

public class CompassImportExtensionsResolvingTest extends SassExtensionsBaseTest {
  @Test
  public void testCompassFiles_1() {
    final PsiElement resolve = resolve();
    assertInstanceOf(resolve, SassScssStylesheetFile.class);
    assertAncestorOfGem(resolve, "compass");
  }

  @Test
  public void testCompassFiles_2() {
    final PsiElement resolve = resolve();
    assertInstanceOf(resolve, SassScssStylesheetFile.class);
    assertAncestorOfGem(resolve, "compass");
  }

  @Test
  public void testCompassFilesInsideScssErb() {
    final PsiElement resolve = resolve("scss.erb");
    assertInstanceOf(resolve, SassScssStylesheetFile.class);
    assertAncestorOfGem(resolve, "compass");
  }

  @NotNull
  @Override
  protected String getTestDataRelativePath() {
    return "resolving";
  }
}
