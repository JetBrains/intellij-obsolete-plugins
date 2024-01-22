package com.intellij.jboss.bpmn.jbpm.model.xml.bpmn20;

import com.intellij.ide.presentation.Presentation;
import com.intellij.jboss.bpmn.jbpm.render.pictures.RenderIcon;
import com.intellij.util.xml.GenericAttributeValue;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * http://www.omg.org/spec/BPMN/20100524/MODEL:tUserTask interface.
 */
@Presentation(icon = "com.intellij.jboss.bpmn.JbossJbpmIcons.Bpmn.Task")
@RenderIcon(icon = "com.intellij.jboss.bpmn.JbossJbpmIcons$Bpmn$Tasks.UserTask")
public interface TUserTask extends Bpmn20DomElement, TTask {
  @NotNull
  GenericAttributeValue<String> getImplementation();

  @NotNull
  List<TRendering> getRenderings();
}
