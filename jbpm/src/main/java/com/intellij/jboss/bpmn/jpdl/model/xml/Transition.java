package com.intellij.jboss.bpmn.jpdl.model.xml;

import com.intellij.jboss.bpmn.jpdl.model.xml.converters.JpdlTransitionTargetConverter;
import com.intellij.util.xml.Convert;
import com.intellij.util.xml.GenericAttributeValue;
import com.intellij.util.xml.GenericDomValue;
import com.intellij.util.xml.Required;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * http://jbpm.org/4.3/jpdm:transitionElemType interface.
 * <pre>
 * <h3>Type http://jbpm.org/4.3/jpdm:transitionType documentation</h3>
 * The outgoing transitions.  The first in the list  will be the default outgoing transition.
 * </pre>
 */
public interface Transition extends EventListenerOwner, InvokersOwner, JpdlDomElement {

  /**
   * Name of the destination activity of this transition.
   *
   * @return the value of the to child.
   */
  @NotNull
  @Required
  @Convert(value = JpdlTransitionTargetConverter.class)
  GenericAttributeValue<JpdlNamedActivity> getTo();

  @NotNull
  GenericAttributeValue<String> getName();

  /**
   * Graphical information used by process designer tool.
   */
  @NotNull
  GenericAttributeValue<String> getG();

  @NotNull
  List<GenericDomValue<String>> getDescriptions();

  @NotNull
  List<Condition> getConditions();
}
