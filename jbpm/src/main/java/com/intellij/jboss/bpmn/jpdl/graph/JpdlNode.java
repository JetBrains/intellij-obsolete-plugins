package com.intellij.jboss.bpmn.jpdl.graph;

import com.intellij.util.xml.DomElement;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public interface JpdlNode<T extends DomElement> {
  @Nullable @Nls String getName();

  @NotNull
  JpdlNodeType getNodeType();

  Icon getIcon();

  @NotNull
  T getIdentifyingElement();
}
