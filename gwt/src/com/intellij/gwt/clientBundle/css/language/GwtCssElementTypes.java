package com.intellij.gwt.clientBundle.css.language;

import com.intellij.psi.css.impl.util.CssStylesheetLazyElementType;
import com.intellij.psi.tree.IElementType;

public final class GwtCssElementTypes {
  public static final IElementType CSS_DEF = new GwtCssElementType("GWT_CSS_DEF");
  public static final IElementType CSS_IF_STATEMENT = new GwtCssElementType("GWT_CSS_IF");
  public static final IElementType CSS_EVAL = new GwtCssElementType("GWT_CSS_EVAL");
  public static final IElementType CSS_EXTERNAL = new GwtCssElementType("GWT_CSS_EXTERNAL");
  public static final IElementType CSS_URL_DECLARATION = new GwtCssElementType("GWT_CSS_URL_DECLARATION");
  public static final IElementType CSS_NO_FLIP = new GwtCssElementType("GWT_CSS_NO_FLIP");
  public static final IElementType CSS_SPRITE = new GwtCssElementType("GWT_CSS_SPRITE");

  public static final CssStylesheetLazyElementType GWT_CSS_LAZY_STYLESHEET = new CssStylesheetLazyElementType("GWT_CSS_LAZY_STYLESHEET", GwtCssLanguage.GWT_CSS_LANGUAGE);

}
