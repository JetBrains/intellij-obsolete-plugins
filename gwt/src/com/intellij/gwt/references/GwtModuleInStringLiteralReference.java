package com.intellij.gwt.references;

import com.intellij.psi.PsiLiteralExpression;
import org.jetbrains.annotations.Nullable;

public class GwtModuleInStringLiteralReference extends GwtModuleReference<PsiLiteralExpression> {
  public GwtModuleInStringLiteralReference(final PsiLiteralExpression element, boolean outputName) {
    super(element, outputName);
  }

  @Override
  protected @Nullable String getStringValue() {
    Object value = myElement.getValue();
    return value instanceof String ? (String)value : null;
  }
}
