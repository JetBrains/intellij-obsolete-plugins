package com.intellij.jboss.bpmn.jbpm.model.xml.bpmn20;

import com.intellij.jboss.bpmn.jbpm.model.render.EventImageProvider;
import com.intellij.jboss.bpmn.jbpm.model.render.SquareImage48x48;
import com.intellij.jboss.bpmn.jbpm.render.pictures.RenderImage;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * http://www.omg.org/spec/BPMN/20100524/MODEL:tEvent interface.
 */

@RenderImage(imageProvider = EventImageProvider.class)
public interface TEvent extends Bpmn20DomElement, TFlowNode, SquareImage48x48 {

  @NotNull
  List<TProperty> getProperties();
}
