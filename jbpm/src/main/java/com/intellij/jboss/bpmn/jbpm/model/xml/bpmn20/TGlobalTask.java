package com.intellij.jboss.bpmn.jbpm.model.xml.bpmn20;

import com.intellij.util.xml.SubTagList;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * http://www.omg.org/spec/BPMN/20100524/MODEL:tGlobalTask interface.
 */
public interface TGlobalTask extends Bpmn20DomElement, TCallableElement {

  @NotNull
  @SubTagList("resourceRole")
  List<TResourceRole> getResourceRoles();
}
