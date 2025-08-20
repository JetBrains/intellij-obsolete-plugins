package com.intellij.jboss.bpmn.jbpm.model.xml.bpmn20;

import com.intellij.util.xml.Convert;
import com.intellij.util.xml.GenericDomValue;
import com.intellij.util.xml.SubTagList;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * http://www.omg.org/spec/BPMN/20100524/MODEL:tOutputSet interface.
 */
public interface TOutputSet extends Bpmn20DomElement, TBaseElement {
  @NotNull
  @Convert(DataOutputRefConvertor.class)
  @SubTagList("dataOutputRefs")
  List<GenericDomValue<TDataOutput>> getDataOutputRefses();

  @NotNull
  @Convert(OptionalAndWhileExecutingOutputRefConvertor.class)
  @SubTagList("optionalOutputRefs")
  List<GenericDomValue<String>> getOptionalOutputRefses();

  @NotNull
  @Convert(OptionalAndWhileExecutingOutputRefConvertor.class)
  @SubTagList("whileExecutingOutputRefs")
  List<GenericDomValue<String>> getWhileExecutingOutputRefses();

  @NotNull
  @Convert(DataInputRefConvertor.class)
  @SubTagList("inputSetRefs")
  List<GenericDomValue<TDataInput>> getInputSetRefses();
}
