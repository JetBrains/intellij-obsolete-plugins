package com.intellij.jboss.bpmn.jpdl.model.xml;

import org.jetbrains.annotations.NotNull;

import java.util.List;

public interface OnOwner extends JpdlDomElement {
  @NotNull
  List<On> getOns();
}
