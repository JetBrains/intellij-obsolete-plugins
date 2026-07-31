package com.intellij.gwt.clientBundle.css.language;

import com.intellij.gwt.clientBundle.css.language.psi.impl.GwtCssDefImpl;
import com.intellij.gwt.clientBundle.css.language.psi.impl.GwtCssEvalImpl;
import com.intellij.gwt.clientBundle.css.language.psi.impl.GwtCssExternalImpl;
import com.intellij.gwt.clientBundle.css.language.psi.impl.GwtCssIfStatementImpl;
import com.intellij.gwt.clientBundle.css.language.psi.impl.GwtCssNoFlipImpl;
import com.intellij.gwt.clientBundle.css.language.psi.impl.GwtCssSpriteImpl;
import com.intellij.gwt.clientBundle.css.language.psi.impl.GwtCssUrlDeclarationImpl;
import com.intellij.psi.css.impl.CssTreeElementFactory;
import com.intellij.psi.impl.source.tree.CompositeElement;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;

public final class GwtCssTreeElementFactory extends CssTreeElementFactory {
  @Override
  public @NotNull CompositeElement createComposite(@NotNull IElementType type) {
    if (type == GwtCssElementTypes.CSS_DEF) {
      return new GwtCssDefImpl();
    }
    else if (type == GwtCssElementTypes.CSS_EVAL) {
      return new GwtCssEvalImpl();
    }
    else if (type == GwtCssElementTypes.CSS_EXTERNAL) {
      return new GwtCssExternalImpl();
    }
    else if (type == GwtCssElementTypes.CSS_IF_STATEMENT) {
      return new GwtCssIfStatementImpl();
    }
    else if (type == GwtCssElementTypes.CSS_NO_FLIP) {
      return new GwtCssNoFlipImpl();
    }
    else if (type == GwtCssElementTypes.CSS_SPRITE) {
      return new GwtCssSpriteImpl();
    }
    else if (type == GwtCssElementTypes.CSS_URL_DECLARATION) {
      return new GwtCssUrlDeclarationImpl();
    }
    return super.createComposite(type);
  }
}
