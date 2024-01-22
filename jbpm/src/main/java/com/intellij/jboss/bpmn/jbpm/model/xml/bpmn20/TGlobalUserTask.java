package com.intellij.jboss.bpmn.jbpm.model.xml.bpmn20;

import com.intellij.util.xml.GenericAttributeValue;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * http://www.omg.org/spec/BPMN/20100524/MODEL:tGlobalUserTask interface.
 */
public interface TGlobalUserTask extends Bpmn20DomElement, TGlobalTask {

  @NotNull
  GenericAttributeValue<String> getImplementation();

  List<TRendering> getRenderings();
}
