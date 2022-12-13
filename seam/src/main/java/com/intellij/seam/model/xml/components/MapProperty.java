package com.intellij.seam.model.xml.components;

import com.intellij.seam.model.xml.SeamDomElement;
import com.intellij.util.xml.GenericDomValue;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * http://jboss.com/products/seam/components:mapProperty interface.
 */
public interface MapProperty extends SeamDomElement {

  @NotNull
  List<SeamValue> getKeys();

  GenericDomValue<SeamValue> addKey();

  @NotNull
  List<SeamValue> getValues();

  GenericDomValue<SeamValue> addValue();}
