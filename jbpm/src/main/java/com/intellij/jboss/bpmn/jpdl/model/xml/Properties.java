package com.intellij.jboss.bpmn.jpdl.model.xml;

import com.intellij.jboss.bpmn.jpdl.model.xml.wireObjectGroup.Property;
import com.intellij.util.xml.GenericAttributeValue;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * http://jbpm.org/4.3/jpdm:propertiesElemType interface.
 */
public interface Properties extends JpdlDomElement {

  /**
   * Returns the value of the file child.
   * <pre>
   * <h3>Attribute null:file documentation</h3>
   * A file on the file system
   * </pre>
   *
   * @return the value of the file child.
   */
  @NotNull
  GenericAttributeValue<String> getFile();


  /**
   * Returns the value of the resource child.
   * <pre>
   * <h3>Attribute null:resource documentation</h3>
   * A file as a resource in the classpath
   * </pre>
   *
   * @return the value of the resource child.
   */
  @NotNull
  GenericAttributeValue<String> getResource();


  /**
   * Returns the value of the url child.
   * <pre>
   * <h3>Attribute null:url documentation</h3>
   * the contents is fetched by loading a url
   * </pre>
   *
   * @return the value of the url child.
   */
  @NotNull
  GenericAttributeValue<String> getUrl();


  /**
   * Returns the value of the is-xml child.
   * <pre>
   * <h3>Attribute null:is-xml documentation</h3>
   * optionally indicates if the content of referenced file in attributes
   *             'file', 'resource' or 'url' is XML.  The default is the
   *             plain properties format with a space or the equals character (=) separating key and value on
   *             each line.
   * </pre>
   *
   * @return the value of the is-xml child.
   */
  @NotNull
  GenericAttributeValue<BooleanValue> getIsXml();


  /**
   * Returns the list of property children.
   *
   * @return the list of property children.
   */
  @NotNull
  List<Property> getProperties();

  /**
   * Adds new child to the list of property children.
   *
   * @return created child
   */
  Property addProperty();
}
