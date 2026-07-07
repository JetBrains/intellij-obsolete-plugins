package org.jetbrains.plugins.ruby.chef.lint.foodcritic;

import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;

class FoodcriticState {
  final @NotNull PsiDirectory cookbook;
  final @NotNull PsiFile file;

  FoodcriticState(final @NotNull PsiDirectory cookbook, final @NotNull PsiFile file) {
    this.cookbook = cookbook;
    this.file = file;
  }
}
