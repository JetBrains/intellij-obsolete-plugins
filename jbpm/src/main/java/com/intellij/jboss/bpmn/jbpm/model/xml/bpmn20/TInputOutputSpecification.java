package com.intellij.jboss.bpmn.jbpm.model.xml.bpmn20;

import com.intellij.util.xml.Required;
import com.intellij.util.xml.SubTagList;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * http://www.omg.org/spec/BPMN/20100524/MODEL:tInputOutputSpecification interface.
 */
public interface TInputOutputSpecification extends Bpmn20DomElement, TBaseElement {

  @NotNull
  @SubTagList("dataInput")
  List<TDataInput> getDataInputs();

  @NotNull
  @SubTagList("dataOutput")
  List<TDataOutput> getDataOutputs();

  @NotNull
  @SubTagList("inputSet")
  @Required
  List<TInputSet> getInputSets();

  @NotNull
  @SubTagList("outputSet")
  @Required
  List<TOutputSet> getOutputSets();
}
