package com.intellij.jboss.bpmn.jpdl.model.xml;

import com.intellij.jboss.bpmn.jpdl.model.xml.converters.EventNameConverter;
import com.intellij.util.xml.Convert;
import com.intellij.util.xml.GenericAttributeValue;
import org.jetbrains.annotations.NotNull;

/**
 * http://jbpm.org/4.3/jpdm:onElemType interface.
 */
public interface On extends EventListenerOwner, InvokersOwner, JpdlDomElement {

  /**
   * The event identification.  start, end, take or any other custom event.
   */
  @NotNull
  @Convert(value = EventNameConverter.class, soft = true)
  GenericAttributeValue<String> getEvent();

  @NotNull
  GenericAttributeValue<Continue> getContinue();


  @NotNull
  Timer getTimer();
}
