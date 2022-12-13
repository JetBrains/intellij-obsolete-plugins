package com.intellij.seam.pages.graph.beans;

import com.intellij.seam.pages.xml.pages.Page;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PageNode extends BasicPagesNode<Page> {
  public PageNode(@NotNull Page identifyingElement, @Nullable @Nls String name) {
    super(identifyingElement, name);
  }
}
