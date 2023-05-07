package com.intellij.play.utils;

import com.intellij.play.language.psi.PlayPsiFile;
import com.intellij.psi.*;
import com.intellij.psi.scope.PsiScopeProcessor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.typedef.members.GrMethod;
import org.jetbrains.plugins.groovy.lang.resolve.NonCodeMembersContributor;

public class PlayClassExtensionsMembersContributor extends NonCodeMembersContributor {

  @Override
  public void processDynamicElements(@NotNull PsiType qualifierType,
                                     @Nullable PsiClass aClass,
                                     @NotNull PsiScopeProcessor processor,
                                     @NotNull PsiElement place,
                                     @NotNull ResolveState state) {

    if (place.getContainingFile().getOriginalFile() instanceof PlayPsiFile) {
      if (aClass != null) {
        for (GrMethod grMethod : PlayClassExtensions.getExtensions(aClass)) {
          if (!processor.execute(grMethod, state)) return;
        }
      }
      if (qualifierType instanceof PsiArrayType &&
          CommonClassNames.JAVA_LANG_STRING.equals(((PsiArrayType)qualifierType).getComponentType().getCanonicalText())) {
        for (GrMethod grMethod : PlayClassExtensions.getStringArrayExtensions(place.getProject())) {
          if (!processor.execute(grMethod, state)) return;
        }
      }
    }
  }
}
