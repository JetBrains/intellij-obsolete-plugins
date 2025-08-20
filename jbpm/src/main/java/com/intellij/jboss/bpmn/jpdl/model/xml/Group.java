package com.intellij.jboss.bpmn.jpdl.model.xml;

import com.intellij.util.xml.GenericAttributeValue;
import com.intellij.util.xml.GenericDomValue;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public interface Group extends ActivitiesContainer, TransitionOwner, JpdlNamedActivity, InvokersOwner, Graphical, OnOwner {

  @NotNull
  GenericAttributeValue<Continue> getContinue();

  @NotNull
  GenericDomValue<String> getDescription();

  @NotNull
  List<Timer> getTimers();
}
