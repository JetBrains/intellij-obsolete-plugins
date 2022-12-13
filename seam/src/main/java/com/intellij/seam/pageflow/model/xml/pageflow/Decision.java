package com.intellij.seam.pageflow.model.xml.pageflow;


import com.intellij.ide.presentation.Presentation;
import com.intellij.util.xml.GenericAttributeValue;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@Presentation(icon = "SeamPageflowIcons.Decision", typeName = Decision.DECISION)
public interface Decision extends PageflowNamedElement, EventsOwner, ExceptionHandlerOwner, PageflowTransitionHolder {

  String DECISION = "Decision";

  @NotNull
  GenericAttributeValue<String> getExpression();

  @NotNull
  List<Delegation> getHandlers();

  Delegation addHandler();
}
