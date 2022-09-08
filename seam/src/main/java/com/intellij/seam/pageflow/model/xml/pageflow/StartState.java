
package com.intellij.seam.pageflow.model.xml.pageflow;


import com.intellij.ide.presentation.Presentation;

/**
 * http://jboss.com/products/seam/pageflow:start-stateElemType interface.
 */
@Presentation(icon = "SeamPageflowIcons.Start", typeName = StartState.START_STATE)
public interface StartState extends PageflowNamedElement, EventsOwner, ExceptionHandlerOwner, PageflowTransitionHolder {

  String START_STATE = "Start State";
}
