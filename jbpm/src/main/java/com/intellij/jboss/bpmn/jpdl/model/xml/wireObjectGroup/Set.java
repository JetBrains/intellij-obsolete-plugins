package com.intellij.jboss.bpmn.jpdl.model.xml.wireObjectGroup;

import com.intellij.jboss.bpmn.jpdl.model.xml.BooleanValue;
import com.intellij.jboss.bpmn.jpdl.model.xml.JpdlDomElement;
import com.intellij.jboss.bpmn.jpdl.model.xml.Properties;
import com.intellij.jboss.bpmn.jpdl.model.xml.WireObject;
import com.intellij.util.xml.Attribute;
import com.intellij.util.xml.GenericAttributeValue;
import com.intellij.util.xml.GenericDomValue;
import com.intellij.util.xml.Required;
import org.jetbrains.annotations.NotNull;

/**
 * http://jbpm.org/4.3/jpdm:setElemType interface.
 */
public interface Set extends JpdlDomElement {

  /**
   * Returns the value of the name child.
   * <pre>
   * <h3>Attribute null:name documentation</h3>
   * The name of the object.  It's optional and serves
   *             as an id to refer to this object from other object declarations.  This name can
   *             also be used lookup the object programmatically.
   * </pre>
   *
   * @return the value of the name child.
   */
  @NotNull
  GenericAttributeValue<String> getName();


  /**
   * Returns the value of the class child.
   * <pre>
   * <h3>Attribute null:class documentation</h3>
   * Implementation class for this set.
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
   *             with Collections.synchronizedSet(Set)
   * </pre>
   *
   * @return the value of the synchronized child.
   */
  @NotNull
  GenericAttributeValue<BooleanValue> getSynchronized();


  /**
   * Returns the value of the null child.
   * <pre>
   * <h3>Element http://jbpm.org/4.3/jpdm:null documentation</h3>
   * the null value
   * </pre>
   *
   * @return the value of the null child.
   */
  @NotNull
  @Required
  GenericDomValue<String> getNull();


  /**
   * Returns the value of the ref child.
   * <pre>
   * <h3>Element http://jbpm.org/4.3/jpdm:ref documentation</h3>
   * A reference to an object in the current environment
   * </pre>
   *
   * @return the value of the ref child.
   */
  @NotNull
  @Required
  Ref getRef();


  /**
   * Returns the value of the env-ref child.
   * <pre>
   * <h3>Element http://jbpm.org/4.3/jpdm:env-ref documentation</h3>
   * The current environment.
   * </pre>
   *
   * @return the value of the env-ref child.
   */
  @NotNull
  @Required
  GenericDomValue<String> getEnvRef();


  /**
   * Returns the value of the jndi child.
   * <pre>
   * <h3>Element http://jbpm.org/4.3/jpdm:jndi documentation</h3>
   * A lookup from JNDI through the InitialContext
   * </pre>
   *
   * @return the value of the jndi child.
   */
  @NotNull
  @Required
  Jndi getJndi();


  /**
   * Returns the value of the list child.
   * <pre>
   * <h3>Element http://jbpm.org/4.3/jpdm:list documentation</h3>
   * A java.util.List
   * </pre>
   *
   * @return the value of the list child.
   */
  @NotNull
  @Required
  List getList();


  /**
   * Returns the value of the map child.
   *
   * @return the value of the map child.
   */
  @NotNull
  @Required
  Map getMap();


  /**
   * Returns the value of the set child.
   * <pre>
   * <h3>Element http://jbpm.org/4.3/jpdm:set documentation</h3>
   * A java.util.Set
   * </pre>
   *
   * @return the value of the set child.
   */
  @NotNull
  @Required
  Set getSet();


  /**
   * Returns the value of the properties child.
   * <pre>
   * <h3>Element http://jbpm.org/4.3/jpdm:properties documentation</h3>
   * A java.util.Properties
   * </pre>
   *
   * @return the value of the properties child.
   */
  @NotNull
  @Required
  Properties getProperties();


  /**
   * Returns the value of the object child.
   *
   * @return the value of the object child.
   */
  @NotNull
  @Required
  WireObject getObject();


  /**
   * Returns the value of the string child.
   *
   * @return the value of the string child.
   */
  @NotNull
  @Required
  JpdlString getString();


  /**
   * Returns the value of the byte child.
   * <pre>
   * <h3>Element http://jbpm.org/4.3/jpdm:byte documentation</h3>
   * A java.lang.Byte
   * </pre>
   *
   * @return the value of the byte child.
   */
  @NotNull
  @Required
  Byte getByte();


  /**
   * Returns the value of the char child.
   * <pre>
   * <h3>Element http://jbpm.org/4.3/jpdm:char documentation</h3>
   * A java.lang.Character
   * </pre>
   *
   * @return the value of the char child.
   */
  @NotNull
  @Required
  Char getChar();


  /**
   * Returns the value of the double child.
   * <pre>
   * <h3>Element http://jbpm.org/4.3/jpdm:double documentation</h3>
   * A java.lang.Double
   * </pre>
   *
   * @return the value of the double child.
   */
  @NotNull
  @Required
  Double getDouble();


  /**
   * Returns the value of the false child.
   * <pre>
   * <h3>Element http://jbpm.org/4.3/jpdm:false documentation</h3>
   * java.lang.Boolean.FALSE
   * </pre>
   *
   * @return the value of the false child.
   */
  @NotNull
  @Required
  False getFalse();


  /**
   * Returns the value of the float child.
   * <pre>
   * <h3>Element http://jbpm.org/4.3/jpdm:float documentation</h3>
   * A java.lang.Float
   * </pre>
   *
   * @return the value of the float child.
   */
  @NotNull
  @Required
  Float getFloat();


  /**
   * Returns the value of the int child.
   * <pre>
   * <h3>Element http://jbpm.org/4.3/jpdm:int documentation</h3>
   * A java.lang.Integer
   * </pre>
   *
   * @return the value of the int child.
   */
  @NotNull
  @Required
  Int getInt();


  /**
   * Returns the value of the long child.
   * <pre>
   * <h3>Element http://jbpm.org/4.3/jpdm:long documentation</h3>
   * A java.lang.Long
   * </pre>
   *
   * @return the value of the long child.
   */
  @NotNull
  @Required
  Long getLong();


  /**
   * Returns the value of the short child.
   * <pre>
   * <h3>Element http://jbpm.org/4.3/jpdm:short documentation</h3>
   * a java.lang.Short
   * </pre>
   *
   * @return the value of the short child.
   */
  @NotNull
  @Required
  Short getShort();


  /**
   * Returns the value of the true child.
   * <pre>
   * <h3>Element http://jbpm.org/4.3/jpdm:true documentation</h3>
   * java.lang.Boolean.TRUE
   * </pre>
   *
   * @return the value of the true child.
   */
  @NotNull
  @Required
  True getTrue();
}
