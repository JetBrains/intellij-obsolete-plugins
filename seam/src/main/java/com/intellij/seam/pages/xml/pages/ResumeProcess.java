package com.intellij.seam.pages.xml.pages;


import com.intellij.util.xml.GenericAttributeValue;
import org.jetbrains.annotations.NotNull;

public interface ResumeProcess extends SeamPagesDomElement {

  @NotNull
  GenericAttributeValue<String> getProcessId();
}
