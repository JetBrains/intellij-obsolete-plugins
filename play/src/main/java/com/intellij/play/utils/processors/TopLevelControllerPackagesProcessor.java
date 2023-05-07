package com.intellij.play.utils.processors;

import com.intellij.openapi.util.Key;
import com.intellij.play.utils.PlayUtils;
import com.intellij.play.utils.beans.PlayImplicitVariable;
import com.intellij.psi.*;
import com.intellij.psi.scope.DelegatingScopeProcessor;
import com.intellij.psi.scope.ElementClassHint;
import com.intellij.psi.scope.PsiScopeProcessor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.groovy.lang.resolve.ResolveUtil;

import javax.swing.*;

public class TopLevelControllerPackagesProcessor implements PlayDeclarationsProcessor {

  @Override
  public boolean processElement(PsiScopeProcessor processor, ResolveState state, PsiElement scope) {
    for (final PsiPackage psiPackage : getTopLevelControllerPackages(scope)) {
      String qualifiedName = psiPackage.getQualifiedName();
      String packageName = qualifiedName.substring("controllers.".length());
      PlayImplicitVariable wrapPackage = new PlayImplicitVariable(packageName, PsiTypes.voidType(), psiPackage) {

        @Override
        public Icon getElementIcon(int flags) {
          return psiPackage.getIcon(0);
        }

        @Override
        public boolean processDeclarations(@NotNull PsiScopeProcessor processor,
                                           @NotNull ResolveState state,
                                           PsiElement lastParent,
                                           @NotNull PsiElement place) {
          DelegatingScopeProcessor delegatingScopeProcessor = new DelegatingScopeProcessor(processor) {
            @Override
            public boolean execute(@NotNull PsiElement element, @NotNull ResolveState state) {
              if (element instanceof PsiClass psiClass) {

                return super.execute(PlayImplicitVariablesFactory.createLightClassImplicitVariable(psiClass, psiClass.getName(), true), state);
              }
              return super.execute(element, state);
            }

            @Override
            public <T> T getHint(@NotNull Key<T> hintKey) {
              if (hintKey == ElementClassHint.KEY) {
                return null;
              }
              return super.getHint(hintKey);
            }
          };

          return psiPackage.processDeclarations(delegatingScopeProcessor, state, lastParent,
                                                place);
        }
      };

      if (!ResolveUtil.processElement(processor, wrapPackage, state)) return false;
    }
    return true;
  }

  private static PsiPackage[] getTopLevelControllerPackages(PsiElement scope) {
    final JavaPsiFacade psiFacade = JavaPsiFacade.getInstance(scope.getProject());
    final PsiPackage psiPackage = psiFacade.findPackage(PlayUtils.CONTROLLERS_PKG);
    if (psiPackage != null) {
      return psiPackage.getSubPackages();
    }
    return PsiPackage.EMPTY_ARRAY;
  }
}
