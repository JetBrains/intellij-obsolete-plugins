package com.intellij.gwt.clientBundle.css.language.psi.impl;

import com.intellij.psi.css.CssRuleset;
import com.intellij.psi.css.impl.CssElementTypes;
import com.intellij.psi.css.impl.CssRulesetWrappingElement;
import com.intellij.psi.impl.source.tree.CompositePsiElement;
import com.intellij.psi.impl.source.tree.TreeElement;
import com.intellij.psi.tree.IElementType;
import com.intellij.util.SmartList;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public abstract class GwtCssRulesetWrappingElementBase extends CompositePsiElement implements CssRulesetWrappingElement {
  protected GwtCssRulesetWrappingElementBase(IElementType type) {
    super(type);
  }

  @Override
  public CssRuleset @NotNull [] getRulesets() {
    List<CssRuleset> rulesets = null;

    for (TreeElement node = getFirstChildNode(); node != null; node = node.getTreeNext()) {
      if (node.getElementType() == CssElementTypes.CSS_RULESET_LIST) {
        for (TreeElement child = node.getFirstChildNode(); child != null; child = child.getTreeNext()) {
          if (child.getElementType() == CssElementTypes.CSS_RULESET) {
            if (rulesets == null) {
              rulesets = new SmartList<>();
            }
            rulesets.add(child.getPsi(CssRuleset.class));
          }
        }
      }
    }
    return rulesets != null ? rulesets.toArray(CssRuleset.EMPTY_ARRAY) : CssRuleset.EMPTY_ARRAY;
  }
}
