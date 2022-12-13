package com.intellij.dmserver.artifacts;

import com.intellij.util.PlatformIcons;

public class DMContainerPackagingElementPresentation extends WithModulePackagingElementPresentation {

  public DMContainerPackagingElementPresentation(String moduleId) {
    super(moduleId, "DMContainerPackagingElementPresentation.display.name", PlatformIcons.CONTENT_ROOT_ICON_CLOSED);
  }
}
