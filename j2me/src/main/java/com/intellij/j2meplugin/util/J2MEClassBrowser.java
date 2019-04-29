/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */
package com.intellij.j2meplugin.util;

import com.intellij.execution.configurations.ConfigurationUtil;
import com.intellij.ide.util.TreeClassChooser;
import com.intellij.ide.util.TreeClassChooserFactory;
import com.intellij.j2meplugin.J2MEBundle;
import com.intellij.j2meplugin.module.J2MEModuleProperties;
import com.intellij.j2meplugin.module.settings.MobileApplicationType;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.search.GlobalSearchScope;
import org.jetbrains.annotations.NotNull;


public class J2MEClassBrowser {
  private final TreeClassChooser myClassChooser;

  public J2MEClassBrowser(@NotNull final Module module) {
    final MobileApplicationType mobileApplicationType = J2MEModuleProperties.getInstance(module).getMobileApplicationType();
    final PsiClass psiClass = JavaPsiFacade.getInstance(module.getProject())
      .findClass(mobileApplicationType.getBaseClassName(), GlobalSearchScope.moduleWithLibrariesScope(module));
    myClassChooser =
    TreeClassChooserFactory.getInstance(module.getProject()).createInheritanceClassChooser(
      J2MEBundle.message("run.configuration.klass.to.start", mobileApplicationType.getPresentableClassName()),
      GlobalSearchScope.moduleScope(module),
      psiClass,
      false,
      false,
      ConfigurationUtil.PUBLIC_INSTANTIATABLE_CLASS);
  }

  public void show() {
    myClassChooser.showDialog();
  }

  public void setField(TextFieldWithBrowseButton field) {
    final PsiClass selectedClass = myClassChooser.getSelected();
    if (selectedClass != null) {
      field.setText(selectedClass.getQualifiedName());
    }
  }
}
