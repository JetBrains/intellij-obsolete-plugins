package com.intellij.dmserver.artifacts;

import icons.DmServerSupportIcons;

public class DMCompositePackagingElementPresentation extends WithModulePackagingElementPresentation {

  public DMCompositePackagingElementPresentation(String moduleId) {
    super(moduleId, "DMCompositePackagingElementPresentation.display.name", DmServerSupportIcons.DM);
  }
}
