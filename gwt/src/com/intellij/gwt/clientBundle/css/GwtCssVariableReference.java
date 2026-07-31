package com.intellij.gwt.clientBundle.css;

import com.intellij.gwt.clientBundle.css.language.psi.impl.GwtCssDeclarationElementBase;
import com.intellij.psi.PsiElementResolveResult;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiPolyVariantReferenceBase;
import com.intellij.psi.ResolveResult;
import com.intellij.psi.css.StylesheetFile;
import com.intellij.psi.css.impl.CssTokenImpl;
import com.intellij.util.ArrayUtilRt;
import com.intellij.util.containers.MultiMap;
import org.jetbrains.annotations.NotNull;

public class GwtCssVariableReference extends PsiPolyVariantReferenceBase<CssTokenImpl> {
  public GwtCssVariableReference(@NotNull CssTokenImpl element) {
    super(element, false);
  }

  @Override
  public ResolveResult @NotNull [] multiResolve(boolean incompleteCode) {
    final PsiFile file = myElement.getContainingFile();
    if (file instanceof StylesheetFile) {
      return PsiElementResolveResult.createResults(GwtCssDeclarationsManager.findDeclarations((StylesheetFile)file, getValue(),
                                                                                              GwtCssDeclarationElementBase.class));
    }
    return ResolveResult.EMPTY_ARRAY;
  }

  @Override
  public Object @NotNull [] getVariants() {
    final PsiFile file = myElement.getContainingFile();
    if (file instanceof StylesheetFile) {
      final MultiMap<String, GwtCssDeclarationElementBase> result = new MultiMap<>();
      GwtCssDeclarationsManager.collectDeclarations((StylesheetFile)file, GwtCssDeclarationElementBase.class, result);
      return ArrayUtilRt.toStringArray(result.keySet());
    }
    return ArrayUtilRt.EMPTY_OBJECT_ARRAY;
  }
}
