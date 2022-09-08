package com.intellij.seam.pageflow.model.xml.pageflow;


import com.intellij.ide.presentation.Presentation;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@Presentation(icon = "SeamPageflowIcons.ProcessState", typeName = ProcessState.PROCESS_STATE)
public interface ProcessState extends PageflowNamedElement, EventsOwner, ExceptionHandlerOwner, PageflowTransitionHolder {

  String PROCESS_STATE = "Process State";

  @NotNull
  List<SubProcess> getSubProcesses();

  SubProcess addSubProcess();
}
