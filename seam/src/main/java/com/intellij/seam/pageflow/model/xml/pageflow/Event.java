package com.intellij.seam.pageflow.model.xml.pageflow;


import com.intellij.util.xml.GenericAttributeValue;
import org.jetbrains.annotations.NotNull;

public interface Event extends ActionsOwner, SeamPageflowDomElement {

  @NotNull
  GenericAttributeValue<String> getType();
}
