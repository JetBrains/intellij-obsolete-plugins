package com.intellij.play.completion;

import com.intellij.codeInsight.completion.*;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.play.completion.beans.PlayTagDescriptor;
import com.intellij.play.utils.PlayUtils;
import com.intellij.psi.PsiElement;
import com.intellij.psi.xml.XmlDocument;
import com.intellij.psi.xml.XmlText;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;

import static com.intellij.patterns.PlatformPatterns.psiElement;

public class PlayHtmlCompletionContributor extends CompletionContributor {

  public PlayHtmlCompletionContributor() {
    extend(CompletionType.BASIC, psiElement(),
           new CompletionProvider<>() {

             @Override
             public void addCompletions(@NotNull final CompletionParameters parameters,
                                        @NotNull final ProcessingContext context,
                                        @NotNull final CompletionResultSet result) {

               if (!PlayUtils.isPlayInstalled(parameters.getPosition().getProject())) return;

               final PsiElement position = parameters.getPosition();
               if (position.getParent() instanceof XmlText || position.getParent() instanceof XmlDocument) {
                 for (final PlayTagDescriptor tagDescriptor : PlayCompletionUtils.getTagDescriptors(
                   ModuleUtilCore.findModuleForPsiElement(position))) {
                   result.addElement(PlayCompletionUtils.createLookupElement(tagDescriptor, "#{"));
                 }
               }
             }
           });
  }
}
