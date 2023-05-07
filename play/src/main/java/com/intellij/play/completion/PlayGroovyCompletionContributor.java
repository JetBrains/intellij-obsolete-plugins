/*
 * Copyright (c) 2000-2005 by JetBrains s.r.o. All Rights Reserved.
 * Use is subject to license terms.
 */
package com.intellij.play.completion;

import com.intellij.codeInsight.AutoPopupController;
import com.intellij.codeInsight.completion.*;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.play.PlayIcons;
import com.intellij.play.completion.beans.NameValueDescriptor;
import com.intellij.play.completion.beans.PlayTagDescriptor;
import com.intellij.play.language.PlayElementTypes;
import com.intellij.play.language.PlaySimpleElementTypes;
import com.intellij.play.language.psi.PlayNameValueCompositeElement;
import com.intellij.play.language.psi.PlayTag;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiLiteral;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.ProcessingContext;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;

import java.util.List;

import static com.intellij.patterns.PlatformPatterns.psiElement;

public class PlayGroovyCompletionContributor extends CompletionContributor {
  public PlayGroovyCompletionContributor() {

    extend(CompletionType.BASIC, psiElement().inside(psiElement(PlayElementTypes.TAG_EXPRESSION)),
           new CompletionProvider<>() {

             @Override
             public void addCompletions(@NotNull final CompletionParameters parameters,
                                        @NotNull final ProcessingContext context,
                                        @NotNull final CompletionResultSet result) {


               if (isNameValueExpressionPosition(parameters) || isLiteralExpression(parameters)) return;

               PlayTag tag = PsiTreeUtil.getParentOfType(parameters.getOriginalPosition(), PlayTag.class);
               if (tag != null) {
                 List<String> existingAttrNames = ContainerUtil.mapNotNull(tag.getNameValues(),
                                                                           element -> element.getName());
                 final PlayTagDescriptor tagDescriptor = PlayCompletionUtils.findTagDescriptor(tag);
                 if (tagDescriptor != null) {
                   for (NameValueDescriptor nameValueDescriptor : tagDescriptor.getDescriptors()) {
                     final String name = nameValueDescriptor.getName();
                     if (!StringUtil.isEmptyOrSpaces(name) && !existingAttrNames.contains(name)) {
                       result.addElement(createLookupElement(nameValueDescriptor));
                     }
                   }
                 }
               }
             }
           });
  }



  private static boolean isLiteralExpression(CompletionParameters parameters) {
    final PsiElement originalPosition = parameters.getOriginalPosition();
    return originalPosition != null && PsiTreeUtil.getParentOfType(originalPosition, PsiLiteral.class) != null;

  }
  private static boolean isNameValueExpressionPosition(CompletionParameters parameters) {
    final PsiElement originalPosition = parameters.getOriginalPosition();
    if (originalPosition != null) {
      if (PsiTreeUtil.getParentOfType(originalPosition, PlayNameValueCompositeElement.class) != null) return true;

      if (originalPosition.getNode().getElementType() == PlaySimpleElementTypes.COMMA) {

        PsiElement prevSibling = originalPosition.getPrevSibling();
        if (prevSibling instanceof PlayNameValueCompositeElement) return true;
        while (prevSibling != null) {
          final IElementType elementType = prevSibling.getNode().getElementType();
          if (elementType == PlayElementTypes.ATTR_NAME) return true; // #{list items:<caret>, }
          if (elementType == PlayElementTypes.TAG_NAME) return false;

          prevSibling = prevSibling.getPrevSibling();
        }
      }
    }

    return false;
  }

  private static LookupElement createLookupElement(@NotNull final NameValueDescriptor descriptor) {
    final String name = descriptor.getName();
    assert name != null;
    return LookupElementBuilder.create(name + descriptor.getTailText()).
      withPresentableText(name).bold().
      withIcon(PlayIcons.Play).
      withTypeText(descriptor.getPresentableText(), true).
      withInsertHandler(new InsertHandler<>() {
        @Override
        public void handleInsert(@NotNull InsertionContext context, @NotNull LookupElement item) {
          PsiDocumentManager.getInstance(context.getProject()).commitDocument(context.getEditor().getDocument());

          if (descriptor.isStringExpression()) {
            context.getEditor().getCaretModel().moveToOffset(context.getSelectionEndOffset() - 1);
          }
          else {
            AutoPopupController.getInstance(context.getProject()).scheduleAutoPopup(context.getEditor());
          }
        }
      });
  }
}
