package com.intellij.dmserver.artifacts;

import com.intellij.icons.AllIcons;

public class DMConfigPackagingElementPresentation extends WithModulePackagingElementPresentation {

  public DMConfigPackagingElementPresentation(String moduleId) {
    super(moduleId, "DMConfigPackagingElementPresentation.displayName", AllIcons.FileTypes.Text);
  }
}
