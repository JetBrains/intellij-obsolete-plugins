package com.intellij.play.references;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.util.Pair;
import com.intellij.play.utils.PlayUtils;
import com.intellij.play.utils.routes.RouterLineDescriptor;
import com.intellij.play.utils.routes.RouterUtils;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiReference;
import com.intellij.psi.PsiReferenceProvider;
import com.intellij.psi.util.CachedValueProvider;
import com.intellij.psi.util.CachedValuesManager;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class PlayRoutesPsiReferenceProvider extends PsiReferenceProvider {

  @Override
  public PsiReference @NotNull [] getReferencesByElement(@NotNull PsiElement element, @NotNull ProcessingContext context) {
    if (!PlayUtils.isPlayInstalled(element.getProject())) return PsiReference.EMPTY_ARRAY;

    Set<PsiReference> references = new HashSet<>();
    if (element instanceof PsiFile) {
      final Module module = ModuleUtilCore.findModuleForPsiElement(element);
      if (module != null) {
        Set<RouterLineDescriptor> descriptors = CachedValuesManager.getCachedValue(element,
                                                                                   () -> CachedValueProvider.Result.createSingleDependency(
                                                                                     RouterUtils.getLineDescriptors(element.getText()), element));
        for (RouterLineDescriptor descriptor : descriptors) {
          references.addAll(getActionReferences((PsiFile)element, descriptor.getAction(), module));
        }
      }
    }
    return references.toArray(PsiReference.EMPTY_ARRAY);
  }

  @NotNull
  private static Set<PsiReference> getActionReferences(@NotNull PsiFile psiFile,
                                                       @Nullable Pair<String, Integer> pair,
                                                       @NotNull final Module module) {
    if (pair == null) return Collections.emptySet();

    return PlayControllerActionPsiReferenceProvider.getActionNameReferences(psiFile, module, pair.getFirst(), pair.getSecond());
  }
}
