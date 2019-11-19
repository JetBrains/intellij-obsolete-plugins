package com.jetbrains.plugins.compass;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.css.CssFunction;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.scss.references.SassScssFunctionReference;

public class CompassFunctionExtensionsTest extends CompassTestCase {

  public void testResolvingToFunctionDefinedInSassFile() {
    doResolvingTest(false);
  }

  public void testResolvingCustomFunctionToItself() {
    doResolvingTest(true);
  }

  public void testResolvingFunctionExtensionToItself() {
    doResolvingTest(true);
  }
  
  public void testResolvingFunctionExtensionToItselfWithInterchangableDash() {
    doResolvingTest(true);
  }

  public void testCompletionExtensionFunctions() {
    myFixture.testCompletionVariants(getTestName(true) + ".sass", "append", "append_selector", "selector_append");
  }
  
  public void testCompletionExtensionFunctionsWithInterchangableDash() {
    myFixture.testCompletionVariants(getTestName(true) + ".sass", "adjust-color", "adjust-hue", "adjust-lightness", "adjust-saturation");
  }

  @Nullable
  private PsiElement resolve() {
    CssFunction function = getFunctionAtCaret();
    assertNotNull(function);
    for (PsiReference reference : function.getReferences()) {
      if (reference instanceof SassScssFunctionReference) {
        return reference.resolve();
      }
    }
    return null;
  }

  @Nullable
  private CssFunction getFunctionAtCaret() {
    final PsiElement element = myFixture.getFile().findElementAt(myFixture.getEditor().getCaretModel().getOffset());
    return PsiTreeUtil.getNonStrictParentOfType(element, CssFunction.class);
  }

  private void doResolvingTest(boolean same) {
    myFixture.configureByFile(getTestName(true) + ".sass");
    if (same) {
      assertSame(getFunctionAtCaret(), resolve());
    }
    else {
      assertNotSame(getFunctionAtCaret(), resolve());
    }
  }

  @Override
  protected String getTestDataSubdir() {
    return "extensionsFunction";
  }
}

