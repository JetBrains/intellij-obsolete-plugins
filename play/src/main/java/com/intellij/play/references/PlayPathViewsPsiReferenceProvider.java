package com.intellij.play.references;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.play.utils.PlayPathUtils;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.PsiReferenceProvider;
import com.intellij.psi.impl.source.resolve.reference.impl.providers.FileReferenceSet;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;

public class PlayPathViewsPsiReferenceProvider extends PsiReferenceProvider {
  @Override
  public PsiReference @NotNull [] getReferencesByElement(@NotNull PsiElement element,
                                                         @NotNull ProcessingContext context) {
    final Module module = ModuleUtilCore.findModuleForPsiElement(element);
    if (module == null) return PsiReference.EMPTY_ARRAY;

    final FileReferenceSet set = FileReferenceSet.createSet(element, true, false, false);
    set.addCustomization(FileReferenceSet.DEFAULT_PATH_EVALUATOR_OPTION,
                         psiFile -> {
                           final PsiDirectory viewsDirectory = PlayPathUtils.getViewsDirectory(module);
                           if (viewsDirectory != null) return Collections.singleton(viewsDirectory);

                           return Collections.emptyList();
                         });

    return set.getAllReferences();
  }
}
