package com.intellij.gwt.module.model;

import com.intellij.util.xml.DomElement;
import com.intellij.util.xml.GenericAttributeValue;
import org.jetbrains.annotations.NotNull;

public interface GwtFileSet extends DomElement {
  GenericAttributeValue<String> getName();

  boolean matches(@NotNull String path, boolean caseSensitive);
}
