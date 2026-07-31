package com.intellij.gwt.clientBundle.css.language;

import com.intellij.lang.Language;
import com.intellij.lang.css.CSSLanguage;

public final class GwtCssLanguage extends Language {
  public static final GwtCssLanguage GWT_CSS_LANGUAGE = new GwtCssLanguage();

  private GwtCssLanguage() {
    super(CSSLanguage.INSTANCE, "GWT-CSS", "text/css");
  }
}
