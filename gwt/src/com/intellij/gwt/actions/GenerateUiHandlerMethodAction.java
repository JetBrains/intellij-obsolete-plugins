package com.intellij.gwt.actions;

import com.intellij.codeInsight.generation.actions.BaseGenerateAction;
import com.intellij.gwt.facet.GwtFacet;
import com.intellij.gwt.uiBinder.mapping.UiBinderMappingService;
import com.intellij.psi.PsiClass;

public class GenerateUiHandlerMethodAction extends BaseGenerateAction {
  public GenerateUiHandlerMethodAction() {
    super(new GenerateUiHandlerMethodHandler());
  }

  @Override
  protected boolean isValidForClass(PsiClass targetClass) {
    final GwtFacet gwtFacet = GwtFacet.findFacetByPsiElement(targetClass);
    if (gwtFacet == null) {
      return false;
    }
    return !UiBinderMappingService.getInstance(gwtFacet.getModule()).getUiXmlFiles(targetClass).isEmpty();
  }

}
