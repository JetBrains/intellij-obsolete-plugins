package com.intellij.gwt.clientBundle.css.language.psi.impl;

import com.intellij.gwt.clientBundle.css.language.GwtCssLanguage;
import com.intellij.psi.FileViewProvider;
import com.intellij.psi.css.impl.CssFileImpl;
import org.jetbrains.annotations.NotNull;

public class GwtCssFileImpl extends CssFileImpl {
  public GwtCssFileImpl(@NotNull FileViewProvider viewProvider) {
    super(viewProvider, GwtCssLanguage.GWT_CSS_LANGUAGE);
  }

  @Override
  public String toString() {
    return "GwtCssFile:" + getName();
  }
}
