package com.intellij.jboss.bpmn.jpdl.model.xml;

import com.intellij.jboss.bpmn.jpdl.model.xml.wireObjectGroup.Field;
import com.intellij.jboss.bpmn.jpdl.model.xml.wireObjectGroup.Property;
import com.intellij.util.xml.GenericAttributeValue;
import com.intellij.util.xml.GenericDomValue;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * http://jbpm.org/4.3/jpdm:wireObjectType interface.
 * <pre>
 * <h3>Type http://jbpm.org/4.3/jpdm:wireObjectType documentation</h3>
 * Any java object that will be created by reflection.                                                                        .
 *     <p>There are three main ways to create an object: by constructor or by a factory
 *     object or a static factory method.</p>
 *     <p><b>Using the constructor</b>: Then the {@code class} attribute must provide the
 *     fully qualified class name.  In case another constructor then the default is
 *     targetted, a {@code constructor} child element can be provided.
 *     The {@code factory} attribute should not be provided for using a constructor.
 *     </p>
 *     <p><b>Using a factory</b>: Then the {@code factory} attribute or one
 *     {@code factory} element must be provided along with the attribute
 *     {@code method}.
 *     </p>
 *     <p><b>Using a static factory method</b>: Then the {@code class} and
 *     {@code method} attributes should be specified.  There should be no
 *     reference to a {@code factory} attribute of element.
 *     </p>
 *     <p><b>Construction and initialization</b>: all objects (constructor and factory)
 *     are build in two phases: construction and initialisation phase.  During construction
 *     all is done until a pointer is obtained to the object.  In initialisation, operations
 *     are applied to the object until it is ready to be used.  The separation between
 *     construction and initialisation is made to allow for bidirectional references.  In
 *     that case, at least one of both ends can not be fully initialised before it is injected
 *     into the other end.
 *     </p>
 *     <p><b>Operations</b>: after construction, various operations can be applied to objects
 *     like e.g. direct field injection, injection through setters, method invocation,
 *     enlist with the standard transaction, subscribe the object as a listener to an observable
 *     </p>
 *     <p><b>Arguments</b>: There are two different places for arguments.  The {@code arg}
 *     elements for the factory method invocation should be placed as child elements under the
 *     {@code object} element.  The {@code arg} elements for the constructor should be
 *     placed as child elements under the {@code constructor} element.
 *     </p>
 * </pre>
 */
public interface WireObject extends JpdlDomElement {

  @NotNull
  GenericAttributeValue<String> getExpr();

  @NotNull
  GenericAttributeValue<String> getLang();

  @NotNull
  GenericAttributeValue<String> getFactory();

  @NotNull
  GenericAttributeValue<String> getMethod();

  /**
   * Returns the value of the auto-wire child.
   * Indicates if the member fields and setter properties
   * should be automatically wired based on matching the property names and types with the
   * object names and types
   * </pre>
   *
   * @return the value of the auto-wire child.
   */
  @NotNull
  GenericAttributeValue<String> getAutoWire();

  @NotNull
  List<GenericDomValue<String>> getDescriptions();

  /**
   * <h3>Element http://jbpm.org/4.3/jpdm:factory documentation</h3>
   * Contains one element that describes the factory object.
   *
   * @return the list of factory children.
   */
  @NotNull
  List<Factory> getFactories();

  /**
   * <pre>
   * <h3>Element http://jbpm.org/4.3/jpdm:constructor documentation</h3>
   * Specifies the arguments to use for a non-default constructor.
   * </pre>
   *
   * @return the list of constructor children.
   */
  @NotNull
  List<Constructor> getConstructors();

  /**
   * <h3>Element http://jbpm.org/4.3/jpdm:arg documentation</h3>
   * The factory method arguments.
   *
   * @return the list of arg children.
   */
  @NotNull
  List<Arg> getArgs();

  /**
   * <pre>
   * <h3>Element http://jbpm.org/4.3/jpdm:field documentation</h3>
   * Injects a value into a member field of this object.
   *         Exactly one child element must specify the value.
   * </pre>
   *
   * @return the list of field children.
   */
  @NotNull
  List<Field> getFields();

  /**
   * Returns the list of property children.
   * <pre>
   * <h3>Element http://jbpm.org/4.3/jpdm:property documentation</h3>
   * Injects a value through a setter method. Exactly one child element must specify the value.
   * </pre>
   *
   * @return the list of property children.
   */
  @NotNull
  List<Property> getProperties();

  /**
   * Returns the list of invoke children.
   * <pre>
   * <h3>Element http://jbpm.org/4.3/jpdm:invoke documentation</h3>
   * Invokes a method
   * </pre>
   *
   * @return the list of invoke children.
   */
  @NotNull
  List<Invoke> getInvokes();
}
