package com.intellij.jboss.bpmn.jbpm.model.xml.bpmn20;

import com.intellij.jboss.bpmn.jbpm.model.render.annotation.DefaultNamePrefix;
import com.intellij.jboss.bpmn.jbpm.model.render.annotation.DefinitionKind;
import com.intellij.util.xml.GenericAttributeValue;
import com.intellij.util.xml.GenericDomValue;
import com.intellij.util.xml.Required;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * http://www.omg.org/spec/BPMN/20100524/MODEL:tLinkEventDefinition interface.
 */
@DefinitionKind("Link")
@DefaultNamePrefix("LinkEventDefinition")
public interface TLinkEventDefinition extends Bpmn20DomElement, TEventDefinition, NameAttributedElement {

  @Override
  @NotNull
  @Required
  GenericAttributeValue<String> getName();

  @NotNull
  List<GenericDomValue<String>> getSources();

  @NotNull
  GenericDomValue<String> getTarget();
}
