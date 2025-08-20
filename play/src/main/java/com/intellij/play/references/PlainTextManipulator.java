/*
 * Copyright 2000-2009 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.intellij.play.references;

import com.intellij.openapi.util.TextRange;
import com.intellij.psi.*;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;

public class PlainTextManipulator extends AbstractElementManipulator<PsiPlainText> {
  @Override
  public PsiPlainText handleContentChange(@NotNull PsiPlainText plainText, @NotNull TextRange textRange, String newContent)
    throws IncorrectOperationException {
    final PsiPlainTextFile file = (PsiPlainTextFile)plainText.getParent();
    String oldText = file.getText();
    String newText =
      oldText.substring(0, textRange.getStartOffset()) + newContent + oldText.substring(textRange.getEndOffset());

    final PsiPlainTextFile psiFile =
      (PsiPlainTextFile)PsiFileFactory.getInstance(file.getProject()).createFileFromText("__plain.txt", newText);
    final PsiElement newPlainText = psiFile.getChildren()[0];

    file.getChildren()[0].replace(newPlainText);

    return (PsiPlainText)file.getChildren()[0];
  }
}
