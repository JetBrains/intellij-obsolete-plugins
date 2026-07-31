package com.intellij.gwt.jsinject;

import com.intellij.codeInsight.highlighting.ReadWriteAccessDetector;
import com.intellij.lang.injection.InjectedLanguageManager;
import com.intellij.lang.javascript.findUsages.JSReadWriteAccessDetector;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiLanguageInjectionHost;
import com.intellij.psi.PsiReference;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;

public final class JsGwtReadWriteAccessDetector extends ReadWriteAccessDetector {
  private static JSReadWriteAccessDetector getJSReadWriteAccessDetector() {
    return JSReadWriteAccessDetector.ourInstance;
  }

  @Override
  public boolean isReadWriteAccessible(@NotNull PsiElement element) {
    return element instanceof JSGwtReferenceExpressionImpl;
      //getJSReadWriteAccessDetector().isReadWriteAccessible(element) && GwtFacet.findFacetByPsiElement(element) != null;
  }

  @Override
  public boolean isDeclarationWriteAccess(@NotNull PsiElement element) {
    return getJSReadWriteAccessDetector().isDeclarationWriteAccess(element);
  }

  @Override
  public @NotNull Access getReferenceAccess(@NotNull PsiElement referencedElement, @NotNull PsiReference reference) {
    return getExpressionAccess(reference.getElement());
  }

  @Override
  public @NotNull Access getExpressionAccess(@NotNull PsiElement expression) {
    if (expression instanceof JSGwtReferenceExpressionImpl gwtExpr) {
      GwtClassMemberReference classMemberReference = ContainerUtil.findInstance(expression.getReferences(), GwtClassMemberReference.class);
      if (classMemberReference != null) {
        PsiElement resolvedReference = classMemberReference.resolve();
        PsiLanguageInjectionHost host = InjectedLanguageManager.getInstance(expression.getProject()).getInjectionHost(expression);
        PsiClass psiClass = PsiTreeUtil.getParentOfType(host, PsiClass.class);
        if (resolvedReference instanceof PsiField && psiClass != null && psiClass.isEquivalentTo(((PsiField)resolvedReference).getContainingClass())) {
          Access usage =
            JsPsiUtil.isAccessedForReading(gwtExpr) ? (JsPsiUtil.isAccessedForWriting(gwtExpr) ? Access.ReadWrite : Access.Read)
                                                  : Access.Write;
          return usage;
        }
      }
    }
    return getJSReadWriteAccessDetector().getExpressionAccess(expression);
  }
}
