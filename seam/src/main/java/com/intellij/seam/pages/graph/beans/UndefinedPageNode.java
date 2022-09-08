package com.intellij.seam.pages.graph.beans;

import com.intellij.seam.pages.xml.pages.PagesViewIdOwner;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class UndefinedPageNode extends BasicPagesNode<PagesViewIdOwner>{
  public UndefinedPageNode(@NotNull final PagesViewIdOwner identifyingElement, @Nullable @Nls String name) {
    super(identifyingElement, name);
  }
}
