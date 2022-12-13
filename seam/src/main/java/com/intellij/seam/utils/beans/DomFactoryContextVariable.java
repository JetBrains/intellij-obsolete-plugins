package com.intellij.seam.utils.beans;

import com.intellij.javaee.el.ELExpressionHolder;
import com.intellij.javaee.el.psi.ELExpression;
import com.intellij.javaee.el.psi.ELSelectExpression;
import com.intellij.javaee.el.psi.ELVariable;
import com.intellij.javaee.el.util.ELResolveUtil;
import com.intellij.lang.injection.InjectedLanguageManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.Ref;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiType;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.xml.XmlAttributeValue;
import com.intellij.seam.model.xml.components.SeamDomFactory;
import com.intellij.seam.utils.SeamCommonUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

public class DomFactoryContextVariable extends ContextVariable {
  private final SeamDomFactory myFactory;
  private final Set<ContextVariable> myVars;
  private final Module myModule;
  private static final Key<Boolean> IS_PROCESSING_VAR_TYPE = new Key<>("IS_PROCESSING_VAR_TYPE");

  public DomFactoryContextVariable(SeamDomFactory factory, String factoryName, Set<ContextVariable> vars, Module module) {
    super(factory, factoryName, PsiType.VOID);
    myFactory = factory;
    myVars = vars;
    myModule = module;
  }

  @NotNull
  @Override
  public PsiType getType() {
    final PsiType type = SeamCommonUtils.getFactoryType(myFactory, myVars);

    return type != null ? type : SeamCommonUtils.getObjectClassType(myModule.getProject());
  }

  @Nullable
  public PsiType getELExpressionType() {
    XmlAttributeValue context = myFactory.getValue().getXmlAttributeValue();

    if (context == null || isProcessing(context)) return null;

    final String value = myFactory.getValue().getStringValue();  // aliasing 3.2.7
    if (value != null && SeamCommonUtils.isElText(value)) {
      final Ref<PsiType> injectionType = new Ref<>();

      setProcessing(context, true);

      InjectedLanguageManager.getInstance(context.getProject()).enumerate(context, (injectedPsi, places) -> {
        final PsiElement at = injectedPsi.findElementAt(injectedPsi.getTextLength() - 1);
        final ELExpressionHolder holder = PsiTreeUtil.getParentOfType(at, ELExpressionHolder.class);
        if (holder != null) {
          ELExpression expression = PsiTreeUtil.getChildOfType(holder, ELExpression.class);

          if (expression != null && !isSelfReference(expression, getName())) {
            injectionType.set(ELResolveUtil.resolveContextAsType(expression));
          }
        }
      });

      setProcessing(context, false);

      return injectionType.get();
    }
    return null;
  }

  private static void setProcessing(@NotNull PsiElement context, boolean b) {
    context.putUserData(IS_PROCESSING_VAR_TYPE, b);
  }

  private static boolean isProcessing(@NotNull PsiElement context) {
    final Boolean isProcessing = context.getUserData(IS_PROCESSING_VAR_TYPE);

    return isProcessing != null && isProcessing.booleanValue();
  }

  private static boolean isSelfReference(final ELExpression expression, final String myName) {
    if (expression == null) return false;

    if (expression.getText().equals(myName)) return true;

    PsiElement firstChild = expression.getFirstChild();
    ELVariable var = null;
    if (firstChild instanceof ELVariable) var = (ELVariable)firstChild;
    if (firstChild instanceof ELSelectExpression) var = ((ELSelectExpression)firstChild).getField();


    return var != null && var.getText().equals(myName);
  }
}
