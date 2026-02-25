// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package org.jetbrains.plugins.grails;

import com.intellij.codeInsight.template.FileTypeBasedContextType;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.grails.fileType.GspFileType;
import org.jetbrains.plugins.grails.lang.gsp.psi.gsp.api.GspFile;

public final class GspTemplateContextType extends FileTypeBasedContextType {

  private GspTemplateContextType() {
    super(GrailsBundle.message("language.gsp"), GspFileType.GSP_FILE_TYPE);
  }

  @Override
  public boolean isInContext(@NotNull PsiFile file, int offset) {
    return file instanceof GspFile;
  }
}