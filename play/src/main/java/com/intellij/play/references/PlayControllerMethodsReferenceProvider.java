package com.intellij.play.references;

import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.play.utils.PlayUtils;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.ProcessingContext;
import com.intellij.util.containers.ContainerUtil;
import java.util.HashSet;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

public class PlayControllerMethodsReferenceProvider extends PsiReferenceProvider {
  @Override
  public PsiReference @NotNull [] getReferencesByElement(@NotNull PsiElement element, @NotNull ProcessingContext context) {
    final PsiClass psiClass = PsiTreeUtil.getParentOfType(element, PsiClass.class);

    if (psiClass != null && PlayUtils.isController(psiClass)) {
      return new PsiReference[]{new PsiReferenceBase<>(element) {
        @Override
        public PsiElement resolve() {
          String value = getValue();
          if (!StringUtil.isEmptyOrSpaces(value)) {
            for (PsiMethod method : getControllerMethods()) {
              if (value.equals(method.getName())) return method;
            }
          }
          return null;
        }

        @Override
        public Object @NotNull [] getVariants() {
          return ContainerUtil.map2Array(getControllerMethods(), method -> LookupElementBuilder.create(method).withIcon(method.getIcon(0)));
        }

        private Set<PsiMethod> getControllerMethods() {
          Set<PsiMethod> methods = new HashSet<>();
          for (final PsiMethod psiMethod : psiClass.getAllMethods()) {
            if (psiMethod.hasModifierProperty(PsiModifier.STATIC) && psiMethod.hasModifierProperty(PsiModifier.PUBLIC)) {
              methods.add(psiMethod);
            }
          }
          return methods;
        }
      }};
    }
    return PsiReference.EMPTY_ARRAY;
  }
}
