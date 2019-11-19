package com.jetbrains.plugins.compass.ruby;

import com.intellij.psi.PsiElement;
import com.intellij.psi.css.CssFunction;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.scss.psi.SassScssFunctionDeclaration;
import org.junit.Test;

public class CompassFunctionExtensionsResolvingTest extends SassExtensionsBaseTest {
  @Test
  public void testCompassFunctions() {
    assertAncestorOfGem(resolve(), "compass");
  }

  @Test
  public void testCompassNestedFunctions() {
    assertAncestorOfGem(resolve(), "compass");
  }

  @Test
  public void testCustomCompassFunctions() {
    // custom function should be resolved to itself
    final PsiElement resolve = resolve();
    final PsiElement at = myFixture.getFile().findElementAt(myFixture.getCaretOffset());
    assertEquals(PsiTreeUtil.getNonStrictParentOfType(at, CssFunction.class), resolve);
  }

  @Test
  public void testCompassSassFunctions() {
    final PsiElement resolve = resolve();
    assertInstanceOf(resolve, SassScssFunctionDeclaration.class);
    assertAncestorOfGem(resolve, "compass");
  }

  @NotNull
  @Override
  protected String getTestDataRelativePath() {
    return "resolving";
  }
}
