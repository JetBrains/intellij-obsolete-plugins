// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.guice.actions;

import com.intellij.guice.constants.GuiceClasses;
import com.intellij.ide.actions.JavaCreateTemplateInPackageAction;
import com.intellij.java.library.JavaLibraryUtil;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.PlatformCoreDataKeys;
import com.intellij.openapi.module.Module;
import com.intellij.psi.JavaDirectoryService;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jps.model.java.JavaModuleSourceRootTypes;

import java.util.function.Supplier;

public abstract class AbstractNewGuiceClassAction extends JavaCreateTemplateInPackageAction<PsiClass> {
  protected AbstractNewGuiceClassAction(@NotNull Supplier<String> dynamicText) {
    super(dynamicText, dynamicText, null, JavaModuleSourceRootTypes.SOURCES);
  }

  @Override
  protected @Nullable PsiElement getNavigationElement(@NotNull PsiClass createdElement) {
    return createdElement.getNameIdentifier();
  }

  @Override
  public boolean startInWriteAction() {
    return false;
  }

  @Override
  protected @Nullable PsiClass doCreate(PsiDirectory dir, String className, String templateName) throws IncorrectOperationException {
    return JavaDirectoryService.getInstance().createClass(dir, className, templateName, true);
  }

  @Override
  protected boolean isAvailable(@NotNull DataContext dataContext) {
    if (!super.isAvailable(dataContext)) return false;
    Module module = PlatformCoreDataKeys.MODULE.getData(dataContext);
    return hasGuice(module);
  }

  public static boolean hasGuice(@Nullable Module module) {
    return JavaLibraryUtil.hasLibraryClass(module, GuiceClasses.ABSTRACT_MODULE);
  }
}