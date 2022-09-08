package com.intellij.seam.model.xml.components;

import com.intellij.seam.model.xml.SeamDomElement;
import com.intellij.util.xml.SubTagList;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * http://jboss.com/products/seam/components:multiValuedProperty interface.
 */
public interface MultiValuedProperty extends SeamDomElement {

  @NotNull
  @SubTagList("value")
  List<SeamValue> getValues();

  SeamValue addValue();
}
