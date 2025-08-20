package com.intellij.jboss.bpmn.jpdl.model.xml;

import com.intellij.jboss.bpmn.jpdl.model.xml.wireObjectGroup.Map;
import com.intellij.util.xml.Attribute;
import com.intellij.util.xml.GenericAttributeValue;
import com.intellij.util.xml.GenericDomValue;
import com.intellij.util.xml.Required;
import org.jetbrains.annotations.NotNull;

/**
 * http://jbpm.org/4.3/jpdm:jmsElemType interface.
 */
public interface Jms extends TransitionOwner, JpdlNamedActivity, Graphical, OnOwner {


  /**
   * Returns the value of the continue child.
   * <pre>
   * <h3>Attribute null:continue documentation</h3>
   * To specify async continuations.
   *       sync is the default.
   * </pre>
   *
   * @return the value of the continue child.
   */
  @NotNull
  GenericAttributeValue<Continue> getContinue();


  /**
   * Returns the value of the connection-factory child.
   * <pre>
   * <h3>Attribute null:connection-factory documentation</h3>
   * jndi name of the jms connection factory
   * </pre>
   *
   * @return the value of the connection-factory child.
   */
  @NotNull
  @Attribute("connection-factory")
  GenericAttributeValue<String> getConnectionFactoryAttr();


  /**
   * Returns the value of the destination child.
   * <pre>
   * <h3>Attribute null:destination documentation</h3>
   * jndi name of the destination to send the message to
   * </pre>
   *
   * @return the value of the destination child.
   */
  @NotNull
  @Attribute("destination")
  GenericAttributeValue<String> getDestinationAttr();


  /**
   * Returns the value of the transacted child.
   * <pre>
   * <h3>Attribute null:transacted documentation</h3>
   * indicates whether the message should be sent in a transactional way
   * </pre>
   *
   * @return the value of the transacted child.
   */
  @NotNull
  @Attribute("transacted")
  GenericAttributeValue<BooleanValue> getTransactedAttr();


  /**
   * Returns the value of the acknowledge child.
   * <pre>
   * <h3>Attribute null:acknowledge documentation</h3>
   * indicates the acknowledgment mode
   * </pre>
   *
   * @return the value of the acknowledge child.
   */
  @NotNull
  @Attribute("acknowledge")
  GenericAttributeValue<Acknowledge> getAcknowledgeAttr();


  /**
   * Returns the value of the object child.
   *
   * @return the value of the object child.
   */
  @NotNull
  @Required
  WireObject getObject();


  /**
   * Returns the value of the text child.
   *
   * @return the value of the text child.
   */
  @NotNull
  @Required
  GenericDomValue<String> getText();


  /**
   * Returns the value of the map child.
   *
   * @return the value of the map child.
   */
  @NotNull
  @Required
  Map getMap();

  /**
   * Returns the value of the connection-factory child.
   * <pre>
   * <h3>Attribute null:connection-factory documentation</h3>
   * jndi name of the jms connection factory
   * </pre>
   *
   * @return the value of the connection-factory child.
   */
  @NotNull
  GenericAttributeValue<String> getConnectionFactory();


  /**
   * Returns the value of the destination child.
   * <pre>
   * <h3>Attribute null:destination documentation</h3>
   * jndi name of the destination to send the message to
   * </pre>
   *
   * @return the value of the destination child.
   */
  @NotNull
  GenericAttributeValue<String> getDestination();


  /**
   * Returns the value of the transacted child.
   * <pre>
   * <h3>Attribute null:transacted documentation</h3>
   * indicates whether the message should be sent in a transactional way
   * </pre>
   *
   * @return the value of the transacted child.
   */
  @NotNull
  GenericAttributeValue<BooleanValue> getTransacted();


  /**
   * Returns the value of the acknowledge child.
   * <pre>
   * <h3>Attribute null:acknowledge documentation</h3>
   * indicates the acknowledgment mode
   * </pre>
   *
   * @return the value of the acknowledge child.
   */
  @NotNull
  GenericAttributeValue<Acknowledge> getAcknowledge();
}
