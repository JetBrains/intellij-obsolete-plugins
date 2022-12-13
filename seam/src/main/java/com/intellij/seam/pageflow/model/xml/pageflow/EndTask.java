package com.intellij.seam.pageflow.model.xml.pageflow;


import com.intellij.util.xml.GenericAttributeValue;
import org.jetbrains.annotations.NotNull;

/**
 * http://jboss.com/products/seam/pageflow:end-taskElemType interface.
 */
public interface EndTask extends SeamPageflowDomElement {

  @NotNull
  GenericAttributeValue<String> getTransition();
}
