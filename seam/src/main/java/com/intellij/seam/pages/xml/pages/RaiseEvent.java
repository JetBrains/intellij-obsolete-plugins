package com.intellij.seam.pages.xml.pages;


import com.intellij.seam.model.references.SeamEventTypeReferenceConverter;
import com.intellij.util.xml.GenericAttributeValue;
import com.intellij.util.xml.Referencing;
import com.intellij.util.xml.Required;
import org.jetbrains.annotations.NotNull;

public interface RaiseEvent extends SeamPagesDomElement {

  @NotNull
  @Required
  @Referencing(SeamEventTypeReferenceConverter.class)
  GenericAttributeValue<String> getType();
}
