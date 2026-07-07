package org.jetbrains.plugins.ruby.chef.codeInsight.completion;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.PsiReferenceProvider;
import com.intellij.util.ProcessingContext;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.ruby.ruby.lang.psi.RPsiElement;
import org.jetbrains.plugins.ruby.ruby.lang.psi.RubyPsiUtilCore;

public final class ChefResourceAttributeReferenceProvider extends PsiReferenceProvider {
  private static ChefResourceAttributeReferenceProvider ourInstance;

  private ChefResourceAttributeReferenceProvider() {
  }

  public static ChefResourceAttributeReferenceProvider getInstance() {
    if (ourInstance == null) {
      ourInstance = new ChefResourceAttributeReferenceProvider();
    }
    return ourInstance;
  }

  @Override
  public PsiReference @NotNull [] getReferencesByElement(@NotNull PsiElement element, @NotNull ProcessingContext context) {
    if (!(element instanceof RPsiElement rPsiElement)) {
      return PsiReference.EMPTY_ARRAY;
    }

    final RPsiElement realContext = RubyPsiUtilCore.getRealContext(rPsiElement);
    return ContainerUtil.ar(new ChefResourceAttributeReference(rPsiElement, realContext == null ? rPsiElement : realContext));
  }
}
