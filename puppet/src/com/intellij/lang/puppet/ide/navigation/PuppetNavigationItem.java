package com.intellij.lang.puppet.ide.navigation;

import com.intellij.navigation.ItemPresentation;
import com.intellij.navigation.NavigationItem;
import com.intellij.psi.NavigatablePsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PuppetNavigationItem implements NavigationItem {
  private final @NotNull String myName;
  private final @NotNull NavigatablePsiElement myDelegate;

  public PuppetNavigationItem(@NotNull String name, @NotNull NavigatablePsiElement delegate) {
    myName = name;
    myDelegate = delegate;
  }

  @Override
  public @NotNull String getName() {
    return myName;
  }

  @Override
  public @Nullable ItemPresentation getPresentation() {
    return PuppetItemPresentation.create(myName, myDelegate);
  }

  @Override
  public void navigate(boolean requestFocus) {
    myDelegate.navigate(requestFocus);
  }

  @Override
  public boolean canNavigate() {
    return myDelegate.canNavigate();
  }

  @Override
  public boolean canNavigateToSource() {
    return myDelegate.canNavigateToSource();
  }

  public @NotNull NavigatablePsiElement getDelegate() {
    return myDelegate;
  }
}
