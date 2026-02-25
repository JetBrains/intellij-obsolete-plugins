// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.config;

import com.intellij.lang.properties.IProperty;
import com.intellij.lang.properties.psi.impl.PropertiesFileImpl;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.Nullable;

class GrailsPropertiesFileCache {

  private PropertiesFileImpl myPropertiesFile;

  private long myModificationCount;

  private String myAppName;

  GrailsPropertiesFileCache(GrailsStructure grailsStructure) {
    PropertiesFileImpl prop = null;

    VirtualFile child = grailsStructure.getAppRoot().findChild("application.properties");
    if (child != null) {
      PsiFile file = grailsStructure.getManager().findFile(child);
      if (file instanceof PropertiesFileImpl) {
        prop = (PropertiesFileImpl)file;
      }
    }

    if (prop != null) {
      myPropertiesFile = prop;
      myModificationCount = prop.getModificationStamp();

      IProperty appNameProperty = prop.findPropertyByKey("app.name");
      if (appNameProperty != null) {
        String value = appNameProperty.getValue();
        if (!StringUtil.isEmptyOrSpaces(value)) {
          myAppName = value.trim();
        }
      }
    }
  }

  boolean isOutdated() {
    return myPropertiesFile != null && (!myPropertiesFile.isValid() || myModificationCount != myPropertiesFile.getModificationStamp());
  }

  public @Nullable String getAppName() {
    return myAppName;
  }

}
