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

package com.intellij.play.rename;

import com.intellij.openapi.util.io.FileUtilRt;
import com.intellij.play.language.psi.PlayPsiFile;
import com.intellij.play.utils.PlayPathUtils;
import com.intellij.play.utils.PlayUtils;
import com.intellij.psi.*;
import com.intellij.refactoring.rename.RenamePsiElementProcessor;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public class PlayViewControllerRenameProcessor extends RenamePsiElementProcessor {

  @Override
  public boolean canProcessElement(@NotNull PsiElement element) {
    return isRenaming(element);
  }

  public boolean isRenaming(PsiElement psiElement) {
   return PlayUtils.hasSecondaryElements(psiElement);
  }

  @Override
  public void prepareRenaming(@NotNull PsiElement psiElement, @NotNull String newName, @NotNull Map<PsiElement, String> allRenames) {
    if (psiElement instanceof PsiClass) {
      final PsiDirectory directory = PlayPathUtils.getCorrespondingDirectory((PsiClass)psiElement);
      if (directory != null) {
        allRenames.put(directory, newName);
      }
    }
    else if (psiElement instanceof PsiMethod) {
      final PsiFile view = PlayPathUtils.getCorrespondingView((PsiMethod)psiElement);
      if (view != null) {
          allRenames.put(view, newName + "." + FileUtilRt.getExtension(view.getName()));
      }
    } else  if (psiElement instanceof PlayPsiFile) {
      final PsiMethod[] methods = PlayPathUtils.getCorrespondingControllerMethods((PsiFile)psiElement);
      for (PsiMethod method : methods) {
        allRenames.put(method, FileUtilRt.getNameWithoutExtension(newName));
      }
    }
  }
}
