package com.intellij.jboss.bpmn.jbpm.model.xml.bpmn20;

import com.intellij.ide.presentation.Presentation;
import com.intellij.jboss.bpmn.jbpm.render.pictures.RenderIcon;
import com.intellij.util.xml.GenericAttributeValue;
import com.intellij.util.xml.GenericDomValue;
import org.jetbrains.annotations.NotNull;

/**
 * http://www.omg.org/spec/BPMN/20100524/MODEL:tScriptTask interface.
 */
@Presentation(icon = "com.intellij.jboss.bpmn.JbossJbpmIcons.Bpmn.Script")
@RenderIcon(icon = "com.intellij.jboss.bpmn.JbossJbpmIcons$Bpmn$Tasks.ScriptTask")
public interface TScriptTask extends Bpmn20DomElement, TTask {

  @NotNull
  GenericAttributeValue<String> getScriptFormat();

  @NotNull
  GenericDomValue<String> getScript();
}
