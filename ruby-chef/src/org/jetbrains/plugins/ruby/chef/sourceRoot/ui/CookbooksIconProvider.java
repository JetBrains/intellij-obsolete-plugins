package org.jetbrains.plugins.ruby.chef.sourceRoot.ui;

import com.intellij.ide.IconProvider;
import com.intellij.openapi.util.Iconable;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;
import icons.RubyChefIcons;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.ruby.chef.ChefUtil;

import javax.swing.Icon;

public final class CookbooksIconProvider extends IconProvider {
  @Override
  public @Nullable Icon getIcon(@NotNull PsiElement element, @Iconable.IconFlags int flags) {
    if (element instanceof PsiDirectory) {
      if (ChefUtil.isCookbook((PsiDirectory)element)) return RubyChefIcons.ChefCookbook;
    }
    return null;
  }
}
