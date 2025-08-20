package com.intellij.play.navigation;

import com.intellij.navigation.GotoRelatedItem;
import com.intellij.navigation.GotoRelatedProvider;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.play.language.psi.PlayPsiFile;
import com.intellij.play.utils.PlayPathUtils;
import com.intellij.play.utils.PlayUtils;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;

public class PlayGotoRelatedFileProvider extends GotoRelatedProvider {
  @NotNull
  @Override
  public List<? extends GotoRelatedItem> getItems(@NotNull PsiElement psiElement) {
    List<GotoRelatedItem> items = new ArrayList<>();

    if (PlayUtils.isPlayInstalled(psiElement.getProject())) {
      // navigate from controller to related views
      PsiClass psiClass = PsiTreeUtil.getParentOfType(psiElement, PsiClass.class);
      if (psiClass != null) {
        final PsiDirectory directory = PlayPathUtils.getCorrespondingDirectory(psiClass);
        if (directory != null) {
          for (PsiFile file : directory.getFiles()) {
            if (file instanceof PlayPsiFile) {
              items.add(new GotoRelatedItem(file));
            }
          }
        }
      }
      // navigate from view to related controller
      PsiFile psiFile = psiElement.getContainingFile();
      final PsiMethod[] controller = PlayPathUtils.getCorrespondingControllerMethods(psiFile);
      for (final PsiMethod method : controller) {
        items.add(new GotoRelatedItem(method) {
          @Override
          public Icon getCustomIcon() {
            return method.getIcon(0);
          }
        });
      }
    }
    return items;
  }

  @NotNull
  @Override
  public List<? extends GotoRelatedItem> getItems(@NotNull DataContext context) {
    return super.getItems(context);
  }
}
