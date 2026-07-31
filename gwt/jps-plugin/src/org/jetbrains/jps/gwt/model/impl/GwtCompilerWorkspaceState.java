package org.jetbrains.jps.gwt.model.impl;

import com.intellij.util.xmlb.annotations.XCollection;

import java.util.ArrayList;
import java.util.List;

public class GwtCompilerWorkspaceState {
  private List<String> myModulesToShowCompilerOutput = new ArrayList<>();

  @XCollection(propertyElementName = "show-output", elementName = "module", valueAttributeName = "name")
  public List<String> getModulesToShowCompilerOutput() {
    return myModulesToShowCompilerOutput;
  }

  public void setModulesToShowCompilerOutput(List<String> modulesToShowCompilerOutput) {
    myModulesToShowCompilerOutput = modulesToShowCompilerOutput;
  }
}
