package com.intellij.seam.pageflow.model.xml.pageflow;

import com.intellij.util.xml.Attribute;
import com.intellij.util.xml.GenericAttributeValue;
import com.intellij.util.xml.GenericDomValue;
import org.jetbrains.annotations.NotNull;

public interface Delegation extends SeamPageflowDomElement {

  @NotNull
  GenericDomValue getValue();

  @NotNull
  @Attribute("class")
  GenericAttributeValue<String> getClazz();

  @NotNull
  GenericAttributeValue<ConfigType> getConfigType();
}
