package com.intellij.seam.pageflow.model.xml.pageflow;


import com.intellij.seam.pageflow.model.xml.converters.PageflowPageElementsConverter;
import com.intellij.util.xml.*;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public interface PageflowDefinition extends ActionsOwner, ExceptionHandlerOwner, EventsOwner, SeamPageflowDomElement {

  @NotNull
  @Required
  @NameValue
  GenericAttributeValue<String> getName();

  @NotNull
  @Attribute("start-page")
  @Convert(value = PageflowPageElementsConverter.class)
  GenericAttributeValue<PageElements> getStartPageAttr();

  @NotNull
  StartState getStartState();

  @NotNull
  StartPage getStartPage();

  @NotNull
  List<Page> getPages();

  Page addPage();

  @NotNull
  List<Decision> getDecisions();

  Decision addDecision();

  @NotNull
  List<ProcessState> getProcessStates();

  ProcessState addProcessState();

  @NotNull
  List<EndState> getEndStates();

  EndState addEndState();
}
