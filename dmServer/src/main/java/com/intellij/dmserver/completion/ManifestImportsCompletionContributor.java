package com.intellij.dmserver.completion;

import com.intellij.codeInsight.completion.*;
import com.intellij.dmserver.facet.DMBundleFacet;
import com.intellij.dmserver.manifest.HeaderValuePartDispatcher;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.PsiElement;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.lang.manifest.ManifestLanguage;
import org.jetbrains.lang.manifest.psi.ManifestTokenType;

public class ManifestImportsCompletionContributor extends CompletionContributor {

  final HeaderValuePartDispatcher<CompletionResultSet, UnitCompleter> ourUnitCompleteDispatcher
    = new HeaderValuePartDispatcher<>(
    new BundleCompleter(),
    new LibraryCompleter(),
    new PackageCompleter()
  );

  public ManifestImportsCompletionContributor() {

    extend(CompletionType.BASIC,
           PlatformPatterns.psiElement(ManifestTokenType.HEADER_VALUE_PART).withLanguage(ManifestLanguage.INSTANCE),
           new CompletionProvider<>() {

             @Override
             public void addCompletions(@NotNull CompletionParameters completionParameters,
                                        @NotNull ProcessingContext processingContext,
                                        @NotNull CompletionResultSet completionResultSet) {
               PsiElement originalPosition = completionParameters.getOriginalPosition();
               if (originalPosition != null && DMBundleFacet.hasDmFacet(originalPosition)) {
                 ourUnitCompleteDispatcher.process(completionParameters.getPosition().getParent(), completionResultSet);
               }
             }
           }
    );
  }
}
