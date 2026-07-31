package com.intellij.gwt.clientBundle.css.language;

import com.intellij.psi.css.impl.CssElementType;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

public final class GwtCssElementType extends IElementType implements CssElementType {
  public GwtCssElementType(@NotNull @NonNls String debugName) {
    super(debugName, GwtCssLanguage.GWT_CSS_LANGUAGE);
  }
}
