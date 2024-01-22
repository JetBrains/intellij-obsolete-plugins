package com.intellij.jboss.bpmn.jbpm.model.xml.bpmn20;

import com.intellij.jboss.bpmn.jbpm.model.converters.ProcessRefConvertor;
import com.intellij.util.xml.Convert;
import com.intellij.util.xml.GenericAttributeValue;
import com.intellij.util.xml.GenericDomValue;
import com.intellij.util.xml.SubTagList;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * http://www.omg.org/spec/BPMN/20100524/MODEL:tParticipant interface.
 */
public interface TParticipant extends Bpmn20DomElement, TBaseElement {
  @NotNull
  @Convert(ProcessRefConvertor.class)
  GenericAttributeValue<String> getProcessRef();

  @NotNull
  @SubTagList("interfaceRef")
  List<GenericDomValue<String>> getInterfaceRefs();

  @NotNull
  @SubTagList("endPointRef")
  List<GenericDomValue<String>> getEndPointRefs();

  @NotNull
  TParticipantMultiplicity getParticipantMultiplicity();
}
