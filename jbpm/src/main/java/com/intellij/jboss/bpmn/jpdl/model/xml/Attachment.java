package com.intellij.jboss.bpmn.jpdl.model.xml;

import com.intellij.util.xml.GenericAttributeValue;
import org.jetbrains.annotations.NotNull;

/**
 * http://jbpm.org/4.3/jpdm:attachmentElemType interface.
 */
public interface Attachment extends JpdlDomElement {

  /**
   * Returns the value of the url child.
   * <pre>
   * <h3>Attribute null:url documentation</h3>
   * URL reference to the attachment
   * </pre>
   *
   * @return the value of the url child.
   */
  @NotNull
  GenericAttributeValue<String> getUrl();


  /**
   * Returns the value of the resource child.
   * <pre>
   * <h3>Attribute null:resource documentation</h3>
   * Name of the attachment resource on the classpath
   * </pre>
   *
   * @return the value of the resource child.
   */
  @NotNull
  GenericAttributeValue<String> getResource();


  /**
   * Returns the value of the file child.
   * <pre>
   * <h3>Attribute null:file documentation</h3>
   * File reference to the attachment
   * </pre>
   *
   * @return the value of the file child.
   */
  @NotNull
  GenericAttributeValue<String> getFile();
}
