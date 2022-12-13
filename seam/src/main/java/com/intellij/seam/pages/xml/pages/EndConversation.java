package com.intellij.seam.pages.xml.pages;


import com.intellij.util.xml.GenericAttributeValue;
import org.jetbrains.annotations.NotNull;

public interface EndConversation extends SeamPagesDomElement {
  @NotNull
  GenericAttributeValue<BeforeRedirect> getBeforeRedirect();


  @NotNull
  GenericAttributeValue<String> getIf();
}
