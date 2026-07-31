package com.intellij.gwt.clientBundle.css.language.psi.impl;

import com.intellij.gwt.clientBundle.css.language.GwtCssElementTypes;
import com.intellij.gwt.clientBundle.css.language.psi.GwtCssSprite;
import com.intellij.psi.css.CssRuleset;
import com.intellij.psi.css.impl.CssElementTypes;
import com.intellij.psi.css.impl.CssRulesetWrappingElement;
import com.intellij.psi.impl.source.tree.CompositePsiElement;
import org.jetbrains.annotations.NotNull;

public class GwtCssSpriteImpl extends CompositePsiElement implements GwtCssSprite, CssRulesetWrappingElement {

  public GwtCssSpriteImpl() {
    super(GwtCssElementTypes.CSS_SPRITE);
  }
  @Override
  public CssRuleset @NotNull [] getRulesets() {
    return getChildrenAsPsiElements(CssElementTypes.CSS_RULESET, CssRuleset.ARRAY_FACTORY);
  }
}
