package com.intellij.jboss.bpmn.jbpm.model.xml.bpmn20;

import com.intellij.util.xml.GenericDomValue;
import com.intellij.util.xml.Required;
import com.intellij.util.xml.SubTagList;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * http://www.omg.org/spec/BPMN/20100524/MODEL:tChoreographyTask interface.
 */
public interface TChoreographyTask extends Bpmn20DomElement, TChoreographyActivity {
  @NotNull
  @SubTagList("messageFlowRef")
  @Required
  List<GenericDomValue<String>> getMessageFlowRefs();

  @SubTagList("messageFlowRef")
  GenericDomValue<String> addMessageFlowRef();
}
