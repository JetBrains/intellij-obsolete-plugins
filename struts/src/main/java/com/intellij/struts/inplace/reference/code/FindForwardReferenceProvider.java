/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.intellij.struts.inplace.reference.code;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.struts.StrutsManager;
import com.intellij.struts.StrutsModel;
import com.intellij.struts.dom.Action;
import com.intellij.struts.dom.Forward;
import com.intellij.struts.inplace.reference.BaseReferenceProvider;
import com.intellij.struts.util.PsiClassUtil;
import com.intellij.util.ProcessingContext;
import com.intellij.util.xml.ElementPresentationManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Provides references to the forward names in calls to {@code actionMapping.findForward()}.
 *
 * @author Yann Cebron
 */
public class FindForwardReferenceProvider extends BaseReferenceProvider {

  @Override
  @NotNull
  public PsiReference[] getReferencesByElement(@NotNull final PsiElement psiElement, @NotNull final ProcessingContext context) {

    final PsiLiteralExpression psiLiteralExpression = (PsiLiteralExpression) psiElement;
    if (!(psiLiteralExpression.getValue() instanceof String)) {
      return PsiReference.EMPTY_ARRAY;
    }

    final Module module = ModuleUtilCore.findModuleForPsiElement(psiElement);
    final StrutsModel model = StrutsManager.getInstance().getCombinedStrutsModel(module);
    if (model == null) {
      return PsiReference.EMPTY_ARRAY;
    }

    final PsiClass myActionClass = PsiTreeUtil.getParentOfType(psiElement, PsiClass.class);
    if (myActionClass == null) {
      return PsiReference.EMPTY_ARRAY; // should not happen
    }
    final String myActionClassName = myActionClass.getQualifiedName();

    final PsiMethod containingMethod = PsiTreeUtil.getParentOfType(psiElement, PsiMethod.class);
    if (containingMethod == null) {
      return PsiReference.EMPTY_ARRAY; // should not happen
    }
    final String myActionMethodName = containingMethod.getName();

    // determine MDA mode
    final boolean mappingDispatchMode = PsiClassUtil.isSuper(myActionClass,
                                                             "org.apache.struts.actions.MappingDispatchAction");

    // determine the list of possible <forward>s for this (sub-)action
    final List<Forward> forwards = new ArrayList<>();
    for (final Action action : model.getActions()) {
      final String actionClassName = action.getType().getStringValue();
      if (actionClassName != null &&
          actionClassName.equals(myActionClassName)) {

        if (!mappingDispatchMode) {
          final List<Forward> list = action.getForwards();
          forwards.addAll(list);
        } else {
          // MappingDispatchAction: <action> "parameter" must equal sub-action method name
          final String param = action.getParameter().getStringValue();
          if (param != null && param.equals(myActionMethodName)) {
            final List<Forward> list = action.getForwards();
            forwards.addAll(list);
          }
        }

      }
    }

    // now add all global forwards
    final List<Forward> globalForwards = model.getGlobalForwards();
    forwards.addAll(globalForwards);

    return new PsiReference[]{new FindForwardReference(psiLiteralExpression, forwards)};
  }


  /**
   * Reference to {@code <forward>} element in {@code struts-config.xml}.
   */
  private static class FindForwardReference extends PsiReferenceBase<PsiLiteralExpression> {

    private final List<Forward> forwards;
    private final String forwardValue;

    /**
     * CTOR.
     *
     * @param psiLiteralExpression Expression to evaluate.
     * @param forwards             All possible forwards to resolve to.
     */
    private FindForwardReference(final PsiLiteralExpression psiLiteralExpression,
                                 final List<Forward> forwards) {
      super(psiLiteralExpression);
      this.forwardValue = (String) myElement.getValue();
      this.forwards = forwards;
    }

    @Override
    @Nullable
    public PsiElement resolve() {
      for (final Forward forward : forwards) {
        if (forwardValue.equals(forward.getName().getStringValue())) {
          return forward.getXmlTag();
        }
      }
      return null;
    }

    @Override
    @NotNull
    public Object[] getVariants() {
      return ElementPresentationManager.getInstance().createVariants(forwards);
    }

  }

}
