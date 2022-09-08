package com.intellij.seam.pageflow.model.xml.pageflow;


import com.intellij.openapi.paths.PathReference;
import com.intellij.util.xml.GenericAttributeValue;
import com.intellij.util.xml.GenericDomValue;
import com.intellij.util.xml.Required;
import org.jetbrains.annotations.NotNull;

public interface PageElements extends ActionsOwner, PageflowNamedElement, EventsOwner, ExceptionHandlerOwner, PageflowTransitionHolder {

  @NotNull
  @Required
  GenericAttributeValue<PathReference> getViewId();

  @NotNull
  GenericDomValue<String> getDescription();

  @NotNull
  GenericDomValue<String> getRedirect();
}
