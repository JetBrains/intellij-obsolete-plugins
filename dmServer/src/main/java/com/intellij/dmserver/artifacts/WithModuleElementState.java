package com.intellij.dmserver.artifacts;

import com.intellij.util.xmlb.annotations.Attribute;

public class WithModuleElementState {
  private String myModuleId;

  @Attribute("module")
  public String getModuleId() {
    return myModuleId;
  }

  public void setModuleId(String moduleId) {
    myModuleId = moduleId;
  }

}
