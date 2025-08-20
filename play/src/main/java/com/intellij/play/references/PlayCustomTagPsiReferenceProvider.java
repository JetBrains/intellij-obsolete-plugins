package com.intellij.play.references;

import com.intellij.jam.JamStringAttributeElement;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.play.completion.PlayCompletionUtils;
import com.intellij.play.completion.beans.PlayFastTagDescriptor;
import com.intellij.play.completion.beans.PlayTagDescriptor;
import com.intellij.play.language.psi.PlayTag;
import com.intellij.play.utils.PlayPathUtils;
import com.intellij.psi.*;
import com.intellij.psi.impl.source.resolve.reference.impl.PsiMultiReference;
import com.intellij.psi.impl.source.resolve.reference.impl.providers.PackageReferenceSet;
import com.intellij.util.ProcessingContext;
import java.util.HashSet;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;

public class PlayCustomTagPsiReferenceProvider extends PsiReferenceProvider {

  @Override
  public PsiReference @NotNull [] getReferencesByElement(@NotNull final PsiElement element,
                                                         @NotNull ProcessingContext context) {
    Set<PsiReference> set = new HashSet<>();
    final PlayTag playTag = (PlayTag)element;
    final String name = playTag.getName();
    if (!StringUtil.isEmptyOrSpaces(name) && isCustomTag(playTag)) {
      if (name.contains(".")) {
        String namespaceName = StringUtil.getPackageName(name);
        String tagName = StringUtil.getShortName(name);
        set.addAll(getNamespaceReferences((PlayTag)element, namespaceName));
        set.add(new PlayCustomTagNamePsiReference((PlayTag)element, tagName, name));
      }
      else {
        set.add(new PlayCustomTagNamePsiReference((PlayTag)element, name, name));
      }
    }

    return set.toArray(PsiReference.EMPTY_ARRAY);
  }

  private static Collection<? extends PsiReference> getNamespaceReferences(@NotNull PlayTag element, @NotNull String namespaceName) {
    final Module module = ModuleUtilCore.findModuleForPsiElement(element);

    if (module == null) return Collections.emptySet();
    Set<PsiReference> references = new HashSet<>();

    Set<PsiReference> descriptors = new HashSet<>();
    final int offset = element.getText().indexOf(namespaceName);
    for (final PlayFastTagDescriptor fastTagDescriptor : PlayPathUtils.getFastTags(module)) {
      if (namespaceName.equals(fastTagDescriptor.getNamespaceValue())){
          descriptors.add(new PsiReferenceBase<>(element, TextRange.create(offset, offset + namespaceName.length())) {

            @Override
            public PsiElement resolve() {
              JamStringAttributeElement<String> namespace = fastTagDescriptor.getNamespace();
              return namespace == null ? null : namespace.getPsiElement();
            }
          });
      }
    }

    if (descriptors.size() > 0) {
      references.add(new PsiMultiReference(descriptors.toArray(PsiReference.EMPTY_ARRAY), element));
    }

    PackageReferenceSet set = new PackageReferenceSet(namespaceName, element, offset) {
      @Override
      public Set<PsiPackage> getInitialContext() {
        Set<PsiPackage> packages = new HashSet<>();

        Set<PsiDirectory> roots = PlayPathUtils.getCustomTagRoots(module);

        for (PsiDirectory root : roots) {
          PsiPackage psiPackage = JavaDirectoryService.getInstance().getPackage(root);
          if (psiPackage != null) {
            packages.add(psiPackage);
          }
        }
        return packages;
      }
    };
    references.addAll(set.getReferences());

    return references;
  }

  private static boolean isCustomTag(PlayTag tag) {
    final String tagName = tag.getName();
    if (!StringUtil.isEmptyOrSpaces(tagName)) {
      for (PlayTagDescriptor descriptor : PlayCompletionUtils.getPredefinedTagDescriptors()) {
        if (tagName.equals(descriptor.getTagName())) return false;
      }
    }
    return true;
  }
}
