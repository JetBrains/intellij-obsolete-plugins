package com.intellij.jboss.bpmn.jpdl.model.xml;

import com.intellij.jboss.bpmn.jpdl.model.xml.wireObjectGroup.Byte;
import com.intellij.jboss.bpmn.jpdl.model.xml.wireObjectGroup.Double;
import com.intellij.jboss.bpmn.jpdl.model.xml.wireObjectGroup.Float;
import com.intellij.jboss.bpmn.jpdl.model.xml.wireObjectGroup.Long;
import com.intellij.jboss.bpmn.jpdl.model.xml.wireObjectGroup.Short;
import com.intellij.jboss.bpmn.jpdl.model.xml.wireObjectGroup.*;
import com.intellij.util.xml.GenericDomValue;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * http://jbpm.org/4.3/jpdm:parametersElemType interface.
 */
public interface Parameters extends JpdlDomElement {

  /**
   * Returns the list of null children.
   * <pre>
   * <h3>Element http://jbpm.org/4.3/jpdm:null documentation</h3>
   * the null value
   * </pre>
   *
   * @return the list of null children.
   */
  @NotNull
  List<GenericDomValue<String>> getNulls();

  /**
   * Adds new child to the list of null children.
   *
   * @return created child
   */
  GenericDomValue<String> addNull();


  /**
   * Returns the list of ref children.
   * <pre>
   * <h3>Element http://jbpm.org/4.3/jpdm:ref documentation</h3>
   * A reference to an object in the current environment
   * </pre>
   *
   * @return the list of ref children.
   */
  @NotNull
  List<Ref> getRefs();

  /**
   * Adds new child to the list of ref children.
   *
   * @return created child
   */
  Ref addRef();


  /**
   * Returns the list of env-ref children.
   * <pre>
   * <h3>Element http://jbpm.org/4.3/jpdm:env-ref documentation</h3>
   * The current environment.
   * </pre>
   *
   * @return the list of env-ref children.
   */
  @NotNull
  List<GenericDomValue<String>> getEnvRefs();

  /**
   * Adds new child to the list of env-ref children.
   *
   * @return created child
   */
  GenericDomValue<String> addEnvRef();


  /**
   * Returns the list of jndi children.
   * <pre>
   * <h3>Element http://jbpm.org/4.3/jpdm:jndi documentation</h3>
   * A lookup from JNDI through the InitialContext
   * </pre>
   *
   * @return the list of jndi children.
   */
  @NotNull
  List<Jndi> getJndis();

  /**
   * Adds new child to the list of jndi children.
   *
   * @return created child
   */
  Jndi addJndi();


  /**
   * Returns the list of list children.
   * <pre>
   * <h3>Element http://jbpm.org/4.3/jpdm:list documentation</h3>
   * A java.util.List
   * </pre>
   *
   * @return the list of list children.
   */
  @NotNull
  List<List> getLists();

  /**
   * Adds new child to the list of list children.
   *
   * @return created child
   */
  List addList();


  /**
   * Returns the list of map children.
   *
   * @return the list of map children.
   */
  @NotNull
  List<Map> getMaps();


  /**
   * Returns the list of set children.
   * <pre>
   * <h3>Element http://jbpm.org/4.3/jpdm:set documentation</h3>
   * A java.util.Set
   * </pre>
   *
   * @return the list of set children.
   */
  @NotNull
  List<Set> getSets();

  /**
   * Adds new child to the list of set children.
   *
   * @return created child
   */
  Set addSet();


  /**
   * Returns the list of properties children.
   * <pre>
   * <h3>Element http://jbpm.org/4.3/jpdm:properties documentation</h3>
   * A java.util.Properties
   * </pre>
   *
   * @return the list of properties children.
   */
  @NotNull
  List<Properties> getPropertieses();

  /**
   * Adds new child to the list of properties children.
   *
   * @return created child
   */
  Properties addProperties();


  /**
   * Returns the list of object children.
   *
   * @return the list of object children.
   */
  @NotNull
  List<WireObject> getObjects();

  /**
   * Adds new child to the list of object children.
   *
   * @return created child
   */
  WireObject addObject();


  /**
   * Returns the list of string children.
   *
   * @return the list of string children.
   */
  @NotNull
  List<JpdlString> getStrings();

  /**
   * Adds new child to the list of string children.
   *
   * @return created child
   */
  String addString();


  /**
   * Returns the list of byte children.
   * <pre>
   * <h3>Element http://jbpm.org/4.3/jpdm:byte documentation</h3>
   * A java.lang.Byte
   * </pre>
   *
   * @return the list of byte children.
   */
  @NotNull
  List<Byte> getBytes();

  /**
   * Adds new child to the list of byte children.
   *
   * @return created child
   */
  Byte addByte();


  /**
   * Returns the list of char children.
   * <pre>
   * <h3>Element http://jbpm.org/4.3/jpdm:char documentation</h3>
   * A java.lang.Character
   * </pre>
   *
   * @return the list of char children.
   */
  @NotNull
  List<Char> getChars();

  /**
   * Adds new child to the list of char children.
   *
   * @return created child
   */
  Char addChar();


  /**
   * Returns the list of double children.
   * <pre>
   * <h3>Element http://jbpm.org/4.3/jpdm:double documentation</h3>
   * A java.lang.Double
   * </pre>
   *
   * @return the list of double children.
   */
  @NotNull
  List<Double> getDoubles();

  /**
   * Adds new child to the list of double children.
   *
   * @return created child
   */
  com.intellij.jboss.bpmn.jpdl.model.xml.wireObjectGroup.Double addDouble();


  /**
   * Returns the list of false children.
   * <pre>
   * <h3>Element http://jbpm.org/4.3/jpdm:false documentation</h3>
   * java.lang.Boolean.FALSE
   * </pre>
   *
   * @return the list of false children.
   */
  @NotNull
  List<False> getFalses();

  /**
   * Adds new child to the list of false children.
   *
   * @return created child
   */
  False addFalse();


  /**
   * Returns the list of float children.
   * <pre>
   * <h3>Element http://jbpm.org/4.3/jpdm:float documentation</h3>
   * A java.lang.Float
   * </pre>
   *
   * @return the list of float children.
   */
  @NotNull
  List<Float> getFloats();

  /**
   * Adds new child to the list of float children.
   *
   * @return created child
   */
  Float addFloat();


  /**
   * Returns the list of int children.
   * <pre>
   * <h3>Element http://jbpm.org/4.3/jpdm:int documentation</h3>
   * A java.lang.Integer
   * </pre>
   *
   * @return the list of int children.
   */
  @NotNull
  List<Int> getInts();

  /**
   * Adds new child to the list of int children.
   *
   * @return created child
   */
  Int addInt();


  /**
   * Returns the list of long children.
   * <pre>
   * <h3>Element http://jbpm.org/4.3/jpdm:long documentation</h3>
   * A java.lang.Long
   * </pre>
   *
   * @return the list of long children.
   */
  @NotNull
  List<Long> getLongs();

  /**
   * Adds new child to the list of long children.
   *
   * @return created child
   */
  Long addLong();


  /**
   * Returns the list of short children.
   * <pre>
   * <h3>Element http://jbpm.org/4.3/jpdm:short documentation</h3>
   * a java.lang.Short
   * </pre>
   *
   * @return the list of short children.
   */
  @NotNull
  List<Short> getShorts();

  /**
   * Adds new child to the list of short children.
   *
   * @return created child
   */
  Short addShort();


  /**
   * Returns the list of true children.
   * <pre>
   * <h3>Element http://jbpm.org/4.3/jpdm:true documentation</h3>
   * java.lang.Boolean.TRUE
   * </pre>
   *
   * @return the list of true children.
   */
  @NotNull
  List<True> getTrues();

  /**
   * Adds new child to the list of true children.
   *
   * @return created child
   */
  True addTrue();
}
