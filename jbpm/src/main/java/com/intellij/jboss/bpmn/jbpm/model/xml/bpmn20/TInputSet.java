package com.intellij.jboss.bpmn.jbpm.model.xml.bpmn20;

import com.intellij.util.xml.Convert;
import com.intellij.util.xml.GenericDomValue;
import com.intellij.util.xml.SubTagList;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * http://www.omg.org/spec/BPMN/20100524/MODEL:tInputSet interface.
 */
public interface TInputSet extends Bpmn20DomElement, TBaseElement {
  @NotNull
  @SubTagList("dataInputRefs")
  @Convert(DataInputRefConvertor.class)
  List<GenericDomValue<TDataInput>> getDataInputRefses();

  @NotNull
  @Convert(OptionalAndWhileExecutingInputRefConvertor.class)
  @SubTagList("optionalInputRefs")
  List<GenericDomValue<TDataInput>> getOptionalInputRefses();

  @NotNull
  @Convert(OptionalAndWhileExecutingInputRefConvertor.class)
  @SubTagList("whileExecutingInputRefs")
  List<GenericDomValue<TDataInput>> getWhileExecutingInputRefses();

  @NotNull
  @Convert(DataOutputRefConvertor.class)
  @SubTagList("outputSetRefs")
  List<GenericDomValue<TDataOutput>> getOutputSetRefses();
}
