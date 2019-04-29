/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */
package com.intellij.j2meplugin.module;

import com.intellij.ide.util.projectWizard.ModuleWizardStep;
import com.intellij.ide.util.projectWizard.WizardContext;
import com.intellij.j2meplugin.J2MEBundle;
import com.intellij.j2meplugin.module.settings.ui.J2MEModuleTypeStep;
import com.intellij.openapi.module.JavaModuleType;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleType;
import com.intellij.openapi.module.ModuleTypeManager;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.roots.ui.configuration.ModulesProvider;
import com.intellij.util.ArrayUtil;
import icons.J2meIcons;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.ArrayList;

public class J2MEModuleType extends ModuleType<J2MEModuleBuilder> {
  @NonNls public static final String ID = "J2ME_MODULE";

  public J2MEModuleType() {
    super(ID);
  }

  public static J2MEModuleType getInstance() {
    return (J2MEModuleType) ModuleTypeManager.getInstance().findByID(ID);
  }

  @Override
  @NotNull
  public J2MEModuleBuilder createModuleBuilder() {
    return new J2MEModuleBuilder();
  }

  @Override
  @NotNull
  public ModuleWizardStep[] createWizardSteps(@NotNull final WizardContext wizardContext, @NotNull final J2MEModuleBuilder moduleBuilder,
                                              @NotNull ModulesProvider modulesProvider) {
    ArrayList<ModuleWizardStep> steps = new ArrayList<>();
    steps.add(new J2MEModuleTypeStep(moduleBuilder, null, "j2me.support.creating.mobile.module"));
    final ModuleWizardStep[] wizardSteps = steps.toArray(ModuleWizardStep.EMPTY_ARRAY);
    return ArrayUtil.mergeArrays(wizardSteps, super.createWizardSteps(wizardContext, moduleBuilder, modulesProvider));
  }

  @Override
  @NotNull
  public String getName() {
    return J2MEBundle.message("module.type.title");
  }

  @Override
  @NotNull
  public String getDescription() {
    return J2MEBundle.message("module.type.description");
  }

  @Override
  public Icon getNodeIcon(boolean isOpened) {
    return J2meIcons.Small_mobile;
  }

  @Override
  public boolean isValidSdk(@NotNull final Module module, final Sdk projectSdk) {
    return JavaModuleType.isValidJavaSdk(module);
  }
}
