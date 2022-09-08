package com.intellij.seam.pageflow.model.xml.pageflow;

import com.intellij.seam.pageflow.model.xml.converters.PageflowDefinitionConverter;
import com.intellij.util.xml.Convert;
import com.intellij.util.xml.GenericAttributeValue;
import com.intellij.util.xml.Required;
import org.jetbrains.annotations.NotNull;


public interface SubProcess extends SeamPageflowDomElement {
  @NotNull
  @Required
  @Convert(value = PageflowDefinitionConverter.class)
  GenericAttributeValue<PageflowDefinition> getName();
}
