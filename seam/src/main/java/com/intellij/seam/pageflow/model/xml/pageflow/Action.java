package com.intellij.seam.pageflow.model.xml.pageflow;


import com.intellij.psi.PsiClass;
import com.intellij.seam.pageflow.model.xml.converters.PageflowDefinitionActionConverter;
import com.intellij.util.xml.Convert;
import com.intellij.util.xml.GenericAttributeValue;
import com.intellij.util.xml.GenericDomValue;
import com.intellij.util.xml.ExtendClass;
import com.intellij.util.xml.Attribute;
import org.jetbrains.annotations.NotNull;

public interface Action extends PageflowNamedElement, SeamPageflowDomElement {

  @NotNull
  GenericDomValue getValue();

  @NotNull
  @Attribute("class")
  @ExtendClass(value = "org.jbpm.graph.def.ActionHandler", instantiatable = false)
  GenericAttributeValue<PsiClass> getClazz();

  @NotNull
  GenericAttributeValue<ConfigType> getConfigType();

  @NotNull
  @Convert(value = PageflowDefinitionActionConverter.class)
  GenericAttributeValue<String> getRefName();

  @NotNull
  GenericAttributeValue<Boolean> getAcceptPropagatedEvents();

  @NotNull
  GenericAttributeValue<String> getExpression();
}
