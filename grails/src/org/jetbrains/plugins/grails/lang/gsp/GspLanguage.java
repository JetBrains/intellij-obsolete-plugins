// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.lang.gsp;

import com.intellij.lang.xml.XMLLanguage;
import com.intellij.psi.templateLanguages.TemplateLanguage;

public final class GspLanguage extends XMLLanguage implements TemplateLanguage {
  public static final GspLanguage INSTANCE = new GspLanguage();

  private GspLanguage() {
    super(XMLLanguage.INSTANCE, "GSP");
  }

  @Override
  public boolean isCaseSensitive() {
    return true;
  }
}
