package com.intellij.seam.pages.graph.beans;

import com.intellij.seam.pages.xml.pages.Render;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class RenderNode extends BasicPagesNode<Render> {

  public RenderNode(@NotNull Render identifyingElement, @Nls @Nullable String name) {
    super(identifyingElement, name);
  }
}

