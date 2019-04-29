/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.intellij.j2meplugin.i18n;

import com.intellij.ide.fileTemplates.CreateFromTemplateActionReplacer;
import com.intellij.ide.fileTemplates.FileTemplate;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.util.Comparing;
import org.jetbrains.annotations.Nullable;

public class CreateMobileResourcePackActionReplacer implements CreateFromTemplateActionReplacer {

  @Override
  @Nullable
  public AnAction replaceCreateFromFileTemplateAction(FileTemplate fileTemplate) {
    if (Comparing.strEqual(fileTemplate.getName(), CreateMobileResourcePackAction.MOBILE_RESOURCE_BUNDLE)) {
      return new CreateMobileResourcePackAction();
    }
    return null;
  }
}