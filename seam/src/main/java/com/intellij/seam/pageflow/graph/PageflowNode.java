package com.intellij.seam.pageflow.graph;

import com.intellij.util.xml.DomElement;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public interface PageflowNode<T extends DomElement> {
  @Nullable @Nls String getName();

  @NotNull
  PageflowNodeType getNodeType();

  Icon getIcon();

  @NotNull
  T getIdentifyingElement();
}
