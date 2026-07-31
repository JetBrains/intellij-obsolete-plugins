package com.intellij.gwt.clientBundle.css.language;

import com.intellij.psi.css.impl.stubs.CssStubDefinition;
import com.intellij.psi.css.impl.stubs.CssStylesheetStubElementType;
import com.intellij.psi.tree.IFileElementType;
import com.intellij.psi.tree.IStubFileElementType;
import org.jetbrains.annotations.NotNull;

public final class GwtCssStubElementTypes {
  public static final CssStylesheetStubElementType GWT_CSS_STYLESHEET =
    new CssStylesheetStubElementType("GWT_CSS_STYLESHEET", GwtCssLanguage.GWT_CSS_LANGUAGE);

  public static final IFileElementType GWT_CSS_FILE = new GwtCssFileElementType();


  private static class GwtCssFileElementType extends IStubFileElementType {

    GwtCssFileElementType() {
      super("GWT_CSS_FILE", GwtCssLanguage.GWT_CSS_LANGUAGE);
    }

    @Override
    public @NotNull String getExternalId() {
      return "gwt.css.file";
    }

    @Override
    public int getStubVersion() {
      return 1 + CssStubDefinition.BASE_VERSION;
    }
  }
}
