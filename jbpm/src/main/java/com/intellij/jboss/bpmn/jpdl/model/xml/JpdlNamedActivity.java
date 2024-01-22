package com.intellij.jboss.bpmn.jpdl.model.xml;

import com.intellij.util.xml.GenericAttributeValue;
import com.intellij.util.xml.NameValue;
import org.jetbrains.annotations.NotNull;

public interface JpdlNamedActivity extends JpdlDomElement {

  /**
   * The id of this activity.  The name should be unique in the complete scope of the process.
   */
  @NotNull
  @NameValue(unique = true)
  GenericAttributeValue<String> getName();
}
