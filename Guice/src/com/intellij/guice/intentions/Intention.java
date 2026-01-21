// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.guice.intentions;

import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.codeInsight.intention.preview.IntentionPreviewInfo;
import com.intellij.openapi.editor.CaretModel;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.intellij.codeInsight.intention.preview.IntentionPreviewInfo.EMPTY;

public abstract class Intention implements IntentionAction{
    private final PsiElementPredicate predicate;

    /**
     * @noinspection AbstractMethodCallInConstructor
     */
    protected Intention(){
        super();
        predicate = getElementPredicate();
    }

    @Override
    public void invoke(@NotNull Project project, Editor editor, PsiFile psiFile)
            throws IncorrectOperationException{
        final PsiElement element = findMatchingElement(psiFile, editor);
        if(element == null){
            return;
        }

        processIntention(element);
    }

    protected abstract void processIntention(@NotNull PsiElement element)
            throws IncorrectOperationException;

    protected abstract @NotNull PsiElementPredicate getElementPredicate();

    public @Nullable PsiElement findMatchingElement(PsiFile file,
                                                    Editor editor){
        final CaretModel caretModel = editor.getCaretModel();
        final int position = caretModel.getOffset();
        PsiElement element = file.findElementAt(position);
        while(element != null){
            if(predicate.satisfiedBy(element)){
                return element;
            } else{
                element = element.getParent();
            }
        }
        return null;
    }

    @Override
    public boolean isAvailable(@NotNull Project project, Editor editor, PsiFile psiFile){
        return findMatchingElement(psiFile, editor) != null;
    }

    @Override
    public boolean startInWriteAction(){
        return true;
    }

  @Override
  public @NotNull IntentionPreviewInfo generatePreview(@NotNull Project project, @NotNull Editor editor, @NotNull PsiFile psiFile) {
    return EMPTY; // todo: ???
  }
}
