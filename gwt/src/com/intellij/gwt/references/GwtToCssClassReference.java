package com.intellij.gwt.references;

import com.intellij.gwt.codeInsight.GwtReferenceUtil;
import com.intellij.gwt.module.GwtModulesManager;
import com.intellij.gwt.module.model.GwtModule;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementResolveResult;
import com.intellij.psi.PsiPolyVariantReferenceBase;
import com.intellij.psi.ResolveResult;
import com.intellij.util.ArrayUtilRt;
import org.jetbrains.annotations.NotNull;

public class GwtToCssClassReference<T extends PsiElement> extends PsiPolyVariantReferenceBase<T> {
  private final TextRange myRangeInElement;

  public GwtToCssClassReference(T element, final TextRange rangeInElement) {
    super(element, true);
    myRangeInElement = rangeInElement;
  }

  @Override
  public @NotNull TextRange getRangeInElement() {
    return myRangeInElement;
  }

  @Override
  public Object @NotNull [] getVariants() {
    final GwtModulesManager modulesManager = GwtModulesManager.getInstance(myElement.getProject());
    final GwtModule module = GwtReferenceUtil.findGwtModule(myElement, modulesManager);
    if (module == null) {
      return ArrayUtilRt.EMPTY_OBJECT_ARRAY;
    }
    return modulesManager.getAllCssClassNames(module);
  }

  @Override
  public ResolveResult @NotNull [] multiResolve(boolean incompleteCode) {
    final GwtModulesManager modulesManager = GwtModulesManager.getInstance(myElement.getProject());
    final GwtModule module = GwtReferenceUtil.findGwtModule(myElement, modulesManager);
    if (module == null) {
      return ResolveResult.EMPTY_ARRAY;
    }
    return PsiElementResolveResult.createResults(modulesManager.findCssClasses(module, getValue()));
  }
}
