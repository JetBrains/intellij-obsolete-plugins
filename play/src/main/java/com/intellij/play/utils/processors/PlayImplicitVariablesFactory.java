package com.intellij.play.utils.processors;

import com.intellij.openapi.util.Key;
import com.intellij.play.utils.PlayUtils;
import com.intellij.play.utils.beans.PlayImplicitVariable;
import com.intellij.psi.*;
import com.intellij.psi.impl.light.LightClass;
import com.intellij.psi.impl.light.LightMethodBuilder;
import com.intellij.psi.impl.light.LightVariableBuilder;
import com.intellij.psi.scope.DelegatingScopeProcessor;
import com.intellij.psi.scope.ElementClassHint;
import com.intellij.psi.scope.PsiScopeProcessor;
import com.intellij.ui.IconManager;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.groovy.lang.psi.impl.synthetic.GrLightVariable;
import org.jetbrains.plugins.groovy.lang.resolve.ResolveUtilKt;

import javax.swing.*;

public final class PlayImplicitVariablesFactory {
  public static PlayImplicitVariable createLightClassImplicitVariable(@NotNull final PsiClass psiClass,
                                                                      @NotNull String name,
                                                                      final boolean processStaticMembers) {
    JavaPsiFacade psiFacade = JavaPsiFacade.getInstance(psiClass.getProject());

    return new PlayImplicitVariable(name,
                                    psiFacade.getElementFactory().createType(createLightClassController(psiClass, processStaticMembers)),
                                    psiClass) {
      @Override
      public Icon getElementIcon(int flags) {
        return psiClass.getIcon(0);
      }
    };
  }

  public static LightClass createLightClassController(final PsiClass psiClass, final boolean processStaticMembers) {
    return new LightClass(psiClass) {
      @Override
      public boolean processDeclarations(@NotNull final PsiScopeProcessor processor,
                                         @NotNull ResolveState state,
                                         PsiElement lastParent,
                                         @NotNull PsiElement place) {
        return super.processDeclarations(new DelegatingScopeProcessor(processor) {
          @Override
          public boolean execute(@NotNull PsiElement element, @NotNull ResolveState state) {
            if (element instanceof PsiMethod psiMethod) {
              if (processStaticMembers && !psiMethod.getModifierList().hasModifierProperty(PsiModifier.STATIC)) return false;

              if (ResolveUtilKt.shouldProcessMethods(processor)) {
                if (!super.execute(getPsiMethodDelegate(psiMethod, true), state)) return false;
                if (!super.execute(getPsiMethodDelegate(psiMethod, false), state)) return false;
              }
              final PsiType returnType = psiMethod.getReturnType();
              if (returnType != null && ResolveUtilKt.shouldProcessLocals(processor)) {
                return super
                  .execute(new GrLightVariable(psiClass.getManager(), psiMethod.getName(), returnType, psiMethod.getNavigationElement()) {
                    @Override
                    public PsiElement setName(@NotNull String name) throws IncorrectOperationException {
                      for (PsiElement declaration : getDeclarations()) {
                        if (declaration instanceof PsiMethod) {
                          ((PsiMethod)declaration).setName(name);
                        }
                      }
                      return getNameIdentifier().replace(new GrLightIdentifier(myManager, name));
                    }
                  }, state);
              }
            }
            else if (element instanceof PsiField) {
              if (processStaticMembers && !(((PsiField)element).hasModifierProperty(PsiModifier.STATIC))) return false;
              if (!PlayUtils.isController(((PsiField)element).getContainingClass())) {
                return super.execute(getPsiFieldDelegate((PsiField)element), state);
              }
            }
            return super.execute(element, state);
          }

          @Override
          public <T> T getHint(@NotNull final Key<T> hintKey) {
            if (hintKey == ElementClassHint.KEY) {
              return (T)new ElementClassHint() {
                @Override
                public boolean shouldProcess(@NotNull DeclarationKind kind) {
                  if (kind == DeclarationKind.METHOD) {
                    return true;
                  }
                  final ElementClassHint hint = processor.getHint(ElementClassHint.KEY);
                  return hint == null || hint.shouldProcess(kind);
                }
              };
            }
            return super.getHint(hintKey);
          }
        }, state, lastParent, place);
      }
    };
  }

  private static LightVariableBuilder getPsiFieldDelegate(PsiField original) {
    return new LightVariableBuilder(original.getName(), original.getType(), original).setBaseIcon(
      IconManager.getInstance().getPlatformIcon(com.intellij.ui.PlatformIcons.Field));
  }

  private static LightMethodBuilder getPsiMethodDelegate(PsiMethod original, boolean withParameters) {
    final LightMethodBuilder delegate = new LightMethodBuilder(original.getManager(), original.getName());
    delegate.setNavigationElement(original);
    delegate.setMethodReturnType(original.getReturnType());
    if (withParameters) {
      for (PsiParameter parameter : original.getParameterList().getParameters()) {
        delegate.addParameter(parameter);
      }
    }
    if (original.hasModifierProperty(PsiModifier.PUBLIC)) {
      delegate.addModifier(PsiModifier.PUBLIC);
    }
    return delegate;
  }
}
