// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.guice.actions;

import com.intellij.guice.GuiceBundle;
import com.intellij.guice.GuiceIcons;
import com.intellij.ide.fileTemplates.FileTemplateDescriptor;
import com.intellij.ide.fileTemplates.FileTemplateGroupDescriptor;
import com.intellij.ide.fileTemplates.FileTemplateGroupDescriptorFactory;

import javax.swing.*;

public final class GuiceTemplatesFactory implements FileTemplateGroupDescriptorFactory {

  @Override
  public FileTemplateGroupDescriptor getFileTemplatesDescriptor() {
    final Icon icon = GuiceIcons.GoogleSmall;
    final FileTemplateGroupDescriptor group = new FileTemplateGroupDescriptor(GuiceBundle.message("group.GuiceActionGroup.text"), icon);

    group.addTemplate(new FileTemplateDescriptor("GuiceNewModule.java", icon));
    group.addTemplate(new FileTemplateDescriptor("GuiceNewBindingAnnotation.java", icon));
    group.addTemplate(new FileTemplateDescriptor("GuiceNewScopeAnnotation.java", icon));
    group.addTemplate(new FileTemplateDescriptor("GuiceNewMethodInterceptor.java", icon));

    return group;
  }
}
