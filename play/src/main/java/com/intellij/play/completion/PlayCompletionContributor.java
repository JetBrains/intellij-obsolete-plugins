/*
 * Copyright (c) 2000-2005 by JetBrains s.r.o. All Rights Reserved.
 * Use is subject to license terms.
 */
package com.intellij.play.completion;

import com.intellij.codeInsight.completion.*;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.play.completion.beans.PlayTagDescriptor;
import com.intellij.play.language.PlayElementTypes;
import com.intellij.play.language.psi.PlayTag;
import com.intellij.psi.PsiElement;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;

import static com.intellij.patterns.PlatformPatterns.psiElement;
import static com.intellij.patterns.StandardPatterns.or;

public class PlayCompletionContributor extends CompletionContributor {

  public PlayCompletionContributor() {
    extend(CompletionType.BASIC, or(psiElement().afterLeaf(psiElement(PlayElementTypes.TAG_START))),
           new CompletionProvider<>() {

             @Override
             public void addCompletions(@NotNull final CompletionParameters parameters,
                                        @NotNull final ProcessingContext context,
                                        @NotNull CompletionResultSet result) {
               final boolean completeTagName = isCompleteTagName(parameters);
               PsiElement position = parameters.getOriginalPosition();
               if (position != null) {
                 PsiElement parent = position.getParent();
                 if (parent instanceof PlayTag) {
                   PsiElement nameElement = ((PlayTag)parent).getNameElement();
                   if (nameElement != null) {
                     final String text = nameElement.getText();
                     final int endIndex = parameters.getOffset() - nameElement.getTextOffset();
                     if (endIndex < text.length()) {
                       String tagNamePrefix = text.substring(0, endIndex);
                       result = result.withPrefixMatcher(tagNamePrefix);
                     }
                   }
                 }
               }
               for (final PlayTagDescriptor tagDescriptor : PlayCompletionUtils
                 .getTagDescriptors(ModuleUtilCore.findModuleForPsiElement(parameters.getOriginalFile()))) {
                 result.addElement(completeTagName
                                   ? PlayCompletionUtils.createTagNameLookupElement(tagDescriptor)
                                   : PlayCompletionUtils.createLookupElement(tagDescriptor));
               }
             }
           });
  }

  private static boolean isCompleteTagName(CompletionParameters parameters) {
    final PsiElement position = parameters.getOriginalPosition();

    return position != null && position.getParent() instanceof PlayTag;
  }
}
