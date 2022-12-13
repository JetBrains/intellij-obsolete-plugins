package com.intellij.seam.pageflow.model.xml.pageflow;


import com.intellij.util.xml.GenericAttributeValue;
import com.intellij.util.xml.GenericDomValue;
import org.jetbrains.annotations.NotNull;

/**
 * http://jboss.com/products/seam/pageflow:scriptElemType interface.
 */
public interface Script extends SeamPageflowDomElement {

  @NotNull
  GenericDomValue getValue();

  @NotNull
  GenericAttributeValue<String> getName();

  @NotNull
  GenericAttributeValue<Boolean> getAcceptPropagatedEvents();
}
