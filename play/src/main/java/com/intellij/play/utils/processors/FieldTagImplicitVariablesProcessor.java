package com.intellij.play.utils.processors;

import com.intellij.play.language.psi.PlayTag;
import com.intellij.play.utils.PlayUtils;
import com.intellij.play.utils.beans.PlayImplicitVariable;
import com.intellij.psi.*;
import com.intellij.psi.impl.light.LightClass;
import com.intellij.psi.scope.PsiScopeProcessor;
import java.util.HashSet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.groovy.lang.resolve.ResolveUtil;

import java.util.Set;

public class FieldTagImplicitVariablesProcessor implements PlayDeclarationsProcessor {

  @Override
  public boolean processElement(PsiScopeProcessor processor, ResolveState state, PsiElement scope) {
    final PlayImplicitVariable fieldVariable = getFieldTagImplicitVariables(scope);
    if (fieldVariable != null) {
      return ResolveUtil.processElement(processor, fieldVariable, state);
    }
    return true;
  }

  @Nullable
  private static PlayImplicitVariable getFieldTagImplicitVariables(@NotNull PsiElement scope) {
    final PlayTag playTag = PlayUtils.getContainingPlayTag(scope);
    if (playTag != null && "field".equals(playTag.getName())) {
      final PsiClass objectClass = PlayUtils.getObjectClass(scope.getProject());
      if (objectClass != null) {
        final PsiElementFactory factory = JavaPsiFacade.getInstance(scope.getProject()).getElementFactory();
        LightClass lightClass = new LightClass(objectClass) {
          @Override
          public boolean processDeclarations(@NotNull PsiScopeProcessor processor,
                                             @NotNull ResolveState state,
                                             PsiElement lastParent,
                                             @NotNull PsiElement place) {

            for (PlayImplicitVariable variable : getInnerFieldVariables()) {
              if (!ResolveUtil.processElement(processor, variable, state)) return true;
            }

            return false;
          }

          @NotNull
          private Set<PlayImplicitVariable> getInnerFieldVariables() {
            Set<PlayImplicitVariable> set = new HashSet<>();

            PsiClassType classType = factory.createType(new LightClass(objectClass) {
              @Override
              public boolean processDeclarations(@NotNull PsiScopeProcessor processor,
                                                 @NotNull ResolveState state,
                                                 PsiElement lastParent,
                                                 @NotNull PsiElement place) {
                return false;
              }
            });

            set.add(new PlayImplicitVariable("id", classType, playTag));
            set.add(new PlayImplicitVariable("name", classType, playTag));
            set.add(new PlayImplicitVariable("value", classType, playTag));
            set.add(new PlayImplicitVariable("error", classType, playTag));
            set.add(new PlayImplicitVariable("errorClass", classType, playTag));

            return set;
          }
        };
        return new PlayImplicitVariable("field", factory.createType(lightClass), playTag);
      }
    }
    return null;
  }
}
