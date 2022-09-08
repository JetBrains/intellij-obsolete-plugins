package com.intellij.seam.pageflow.model.xml.pageflow;

import org.jetbrains.annotations.NotNull;

import java.util.List;

public interface ExceptionHandlerOwner extends SeamPageflowDomElement {
  @NotNull
  List<ExceptionHandler> getExceptionHandlers();

  ExceptionHandler addExceptionHandler();
}
