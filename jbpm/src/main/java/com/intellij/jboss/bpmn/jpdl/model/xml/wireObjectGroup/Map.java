package com.intellij.jboss.bpmn.jpdl.model.xml.wireObjectGroup;

import com.intellij.jboss.bpmn.jpdl.model.xml.BooleanValue;
import com.intellij.jboss.bpmn.jpdl.model.xml.Entry;
import com.intellij.jboss.bpmn.jpdl.model.xml.JpdlDomElement;
import com.intellij.util.xml.Attribute;
import com.intellij.util.xml.GenericAttributeValue;
import org.jetbrains.annotations.NotNull;

/**
 * http://jbpm.org/4.3/jpdm:mapType interface.
 * <pre>
 * <h3>Type http://jbpm.org/4.3/jpdm:mapType documentation</h3>
 * A java.util.Map
 * </pre>
 */
public interface Map extends JpdlDomElement {

  /**
   * Returns the value of the class child.
   * <pre>
   * <h3>Attribute null:class documentation</h3>
   * Implementation class for this map.
   * </pre>
   *
   * @return the value of the class child.
   */
  @NotNull
  @Attribute("class")
  GenericAttributeValue<String> getClazz();


  /**
   * Returns the value of the synchronized child.
   * <pre>
   * <h3>Attribute null:synchronized documentation</h3>
   * Indicates if this collection should be synchronized
   *       with Collections.synchronizedList(List)
   * </pre>
   *
   * @return the value of the synchronized child.
   */
  @NotNull
  GenericAttributeValue<BooleanValue> getSynchronized();


  /**
   * Returns the list of entry children.
   *
   * @return the list of entry children.
   */
  @NotNull
  java.util.List<Entry> getEntries();

  /**
   * Adds new child to the list of entry children.
   *
   * @return created child
   */
  Entry addEntry();
}
