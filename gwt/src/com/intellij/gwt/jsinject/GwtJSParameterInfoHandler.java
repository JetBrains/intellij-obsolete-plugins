package com.intellij.gwt.jsinject;

import com.intellij.codeInsight.CodeInsightBundle;
import com.intellij.codeInsight.hint.api.impls.MethodParameterInfoHandler;
import com.intellij.javascript.JSParameterInfoHandlerKt;
import com.intellij.lang.javascript.JSTokenTypes;
import com.intellij.lang.javascript.psi.JSArgumentsHolder;
import com.intellij.lang.javascript.psi.JSCallExpression;
import com.intellij.lang.javascript.psi.JSExpression;
import com.intellij.lang.parameterInfo.CreateParameterInfoContext;
import com.intellij.lang.parameterInfo.ParameterInfoHandlerWithTabActionSupport;
import com.intellij.lang.parameterInfo.ParameterInfoUIContext;
import com.intellij.lang.parameterInfo.ParameterInfoUtils;
import com.intellij.lang.parameterInfo.UpdateParameterInfoContext;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMember;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiReference;
import com.intellij.psi.PsiSubstitutor;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.Set;

public final class GwtJSParameterInfoHandler implements ParameterInfoHandlerWithTabActionSupport<JSArgumentsHolder, PsiMember, JSExpression> {
  private static final Set<Class<?>> ourStopSearch = Collections.singleton(PsiMember.class);

  @Override
  public @NotNull Set<? extends Class<?>> getArgListStopSearchClasses() {
    return ourStopSearch;
  }

  @Override
  public JSArgumentsHolder findElementForParameterInfo(final @NotNull CreateParameterInfoContext context) {
    JSArgumentsHolder argumentList = JSParameterInfoHandlerKt.findArgumentList(context.getFile(), context.getOffset());
    if (argumentList != null) {
      PsiElement parent = argumentList.getParent();
      if (parent instanceof JSCallExpression) {
        JSExpression expression = ((JSCallExpression)parent).getMethodExpression();
        GwtClassMemberReference gwtReference = getGwtClassMemberReference(expression);
        if (gwtReference != null) {
          PsiElement element = gwtReference.resolve();
          if (element != null) {
            context.setItemsToShow(new Object[]{element});
            return argumentList;
          }
        }
      }
    }
    return null;
  }

  private static @Nullable GwtClassMemberReference getGwtClassMemberReference(@Nullable JSExpression expression) {
    if (expression == null) return null;

    if (!(expression instanceof JSGwtReferenceExpressionImpl)) {
      PsiElement child = expression.getLastChild();
      if (child instanceof JSGwtReferenceExpressionImpl) {
        expression = (JSGwtReferenceExpressionImpl)child;
      }
      else {
        return null;
      }
    }

    PsiReference[] references = expression.getReferences();
    PsiReference last = references[references.length - 1];
    return last instanceof GwtClassMemberReference ? (GwtClassMemberReference)last : null;
  }

  @Override
  public void showParameterInfo(final @NotNull JSArgumentsHolder element, final @NotNull CreateParameterInfoContext context) {
    context.showHint(element, element.getTextOffset(), this);
  }

  @Override
  public JSArgumentsHolder findElementForUpdatingParameterInfo(final @NotNull UpdateParameterInfoContext context) {
    return JSParameterInfoHandlerKt.findArgumentList(context.getFile(), context.getOffset());
  }

  @Override
  public void updateParameterInfo(final @NotNull JSArgumentsHolder parameterOwner, final @NotNull UpdateParameterInfoContext context) {
    context.setCurrentParameter(ParameterInfoUtils.getCurrentParameterIndex(parameterOwner.getNode(), context.getOffset(), JSTokenTypes.COMMA));
  }

  @Override
  public void updateUI(final PsiMember p, final @NotNull ParameterInfoUIContext context) {
    if (p instanceof PsiMethod) {
      MethodParameterInfoHandler.updateMethodPresentation((PsiMethod)p, PsiSubstitutor.EMPTY, context);
    }
    else {
      context.setupUIComponentPresentation(CodeInsightBundle.message("parameter.info.no.parameters"), -1, -1, false, false, false,
                                           context.getDefaultParameterColor());
    }
  }

  @Override
  public JSExpression @NotNull [] getActualParameters(final @NotNull JSArgumentsHolder o) {
    return o.getArguments();
  }

  @Override
  public @NotNull IElementType getActualParameterDelimiterType() {
    return JSTokenTypes.COMMA;
  }

  @Override
  public @NotNull IElementType getActualParametersRBraceType() {
    return JSTokenTypes.RBRACE;
  }

  @Override
  public @NotNull Set<Class<?>> getArgumentListAllowedParentClasses() {
    return Collections.singleton(JSCallExpression.class);
  }

  @Override
  public @NotNull Class<JSArgumentsHolder> getArgumentListClass() {
    return JSArgumentsHolder.class;
  }
}
