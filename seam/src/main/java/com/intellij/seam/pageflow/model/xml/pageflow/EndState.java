package com.intellij.seam.pageflow.model.xml.pageflow;

import com.intellij.ide.presentation.Presentation;

@Presentation(icon = "SeamPageflowIcons.End", typeName = EndState.END_STATE)
public interface EndState extends PageflowNamedElement, EventsOwner, ExceptionHandlerOwner, SeamPageflowDomElement {
  String END_STATE = "End State";
}
