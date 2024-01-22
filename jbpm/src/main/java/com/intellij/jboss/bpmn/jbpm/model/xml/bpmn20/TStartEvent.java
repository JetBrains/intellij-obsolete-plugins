package com.intellij.jboss.bpmn.jbpm.model.xml.bpmn20;

import com.intellij.ide.presentation.Presentation;
import com.intellij.jboss.bpmn.jbpm.model.render.StartEventColorProvider;
import com.intellij.jboss.bpmn.jbpm.model.render.annotation.DefaultNamePrefix;
import com.intellij.jboss.bpmn.jbpm.model.render.annotation.EventKind;
import com.intellij.jboss.bpmn.jbpm.render.label.RenderLabelColor;
import com.intellij.util.xml.GenericAttributeValue;
import org.jetbrains.annotations.NotNull;

/**
 * http://www.omg.org/spec/BPMN/20100524/MODEL:tStartEvent interface.
 */
@Presentation(icon = "com.intellij.jboss.bpmn.JbossJbpmIcons.Bpmn.Start")
@RenderLabelColor(color = StartEventColorProvider.class)
@EventKind("Start")
@DefaultNamePrefix("Start Event")
public interface TStartEvent extends Bpmn20DomElement, TCatchEvent {

  @NotNull
  GenericAttributeValue<Boolean> getIsInterrupting();
}
