package com.intellij.jboss.bpmn.jbpm.model.xml.bpmn20;

import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * http://www.omg.org/spec/BPMN/20100524/MODEL:tLaneSet interface.
 */
public interface TLaneSet extends Bpmn20DomElement, TBaseElement {

  @NotNull
  List<TLane> getLanes();
}
