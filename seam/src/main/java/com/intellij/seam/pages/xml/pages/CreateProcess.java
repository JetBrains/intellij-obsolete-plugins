package com.intellij.seam.pages.xml.pages;


import com.intellij.util.xml.GenericAttributeValue;
import org.jetbrains.annotations.NotNull;

public interface CreateProcess extends SeamPagesDomElement {

  @NotNull
  GenericAttributeValue<String> getDefinition();
}
