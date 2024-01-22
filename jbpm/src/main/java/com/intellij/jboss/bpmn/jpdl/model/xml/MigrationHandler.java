package com.intellij.jboss.bpmn.jpdl.model.xml;

import com.intellij.util.xml.Attribute;
import com.intellij.util.xml.GenericAttributeValue;
import org.jetbrains.annotations.NotNull;

/**
 * http://jbpm.org/4.3/jpdm:migration-handlerElemType interface.
 */
public interface MigrationHandler extends JpdlDomElement {

  /**
   * Returns the value of the class child.
   *
   * @return the value of the class child.
   */
  @NotNull
  @Attribute("class")
  GenericAttributeValue<String> getClazz();
}
