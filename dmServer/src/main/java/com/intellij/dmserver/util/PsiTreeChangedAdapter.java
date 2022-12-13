package com.intellij.dmserver.util;

import com.intellij.psi.PsiTreeChangeAdapter;
import com.intellij.psi.PsiTreeChangeEvent;
import org.jetbrains.annotations.NotNull;

public abstract class PsiTreeChangedAdapter extends PsiTreeChangeAdapter {

  @Override
  public void childAdded(@NotNull PsiTreeChangeEvent event) {
    treeChanged(event);
  }

  @Override
  public void childMoved(@NotNull PsiTreeChangeEvent event) {
    treeChanged(event);
  }

  @Override
  public void childRemoved(@NotNull PsiTreeChangeEvent event) {
    treeChanged(event);
  }

  @Override
  public void childrenChanged(@NotNull PsiTreeChangeEvent event) {
    treeChanged(event);
  }

  @Override
  public void childReplaced(@NotNull PsiTreeChangeEvent event) {
    treeChanged(event);
  }

  @Override
  public void propertyChanged(@NotNull PsiTreeChangeEvent event) {
    treeChanged(event);
  }

  protected abstract void treeChanged(PsiTreeChangeEvent event);
}
