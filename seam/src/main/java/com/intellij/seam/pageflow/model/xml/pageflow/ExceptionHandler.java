package com.intellij.seam.pageflow.model.xml.pageflow;


import com.intellij.psi.PsiClass;
import com.intellij.util.xml.GenericAttributeValue;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * http://jboss.com/products/seam/pageflow:exception-handlerElemType interface.
 */
public interface ExceptionHandler extends SeamPageflowDomElement {
  @NotNull
  GenericAttributeValue<PsiClass> getExceptionClass();

  @NotNull
  List<Action> getActions();

  Action addAction();

  @NotNull
  List<Script> getScripts();

  Script addScript();
}
