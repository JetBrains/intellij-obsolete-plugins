package com.intellij.seam.pageflow.model.xml.pageflow;

import org.jetbrains.annotations.NotNull;
import com.intellij.util.xml.GenericAttributeValue;
import com.intellij.util.xml.NameValue;

public interface PageflowNamedElement extends SeamPageflowDomElement {
  @NotNull
  @NameValue(unique = true)
  GenericAttributeValue<String> getName();
}
