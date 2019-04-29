/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.intellij.j2meplugin.i18n;

import com.intellij.ide.fileTemplates.DefaultCreateFromTemplateHandler;
import com.intellij.ide.fileTemplates.FileTemplate;
import com.intellij.j2meplugin.module.J2MEModuleType;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleType;
import com.intellij.openapi.module.ModuleUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiDirectory;
import org.jetbrains.annotations.NotNull;

public class CreateMobileResourcePackHandler extends DefaultCreateFromTemplateHandler {
  @Override
  public boolean handlesTemplate(@NotNull FileTemplate template) {
    return StringUtil.startsWithIgnoreCase(template.getName(), "Mobile");
  }

  @Override
  public boolean canCreate(@NotNull PsiDirectory[] dirs) {
    for (PsiDirectory dir : dirs) {
      final Module module = ModuleUtil.findModuleForPsiElement(dir);
      if (module != null && ModuleType.get(module) == J2MEModuleType.getInstance()) {
        return true;
      }
    }
    return false;
  }
}