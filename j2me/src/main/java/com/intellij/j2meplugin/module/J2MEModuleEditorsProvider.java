/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */
package com.intellij.j2meplugin.module;

import com.intellij.j2meplugin.module.settings.ui.J2MEModuleConfEditor;
import com.intellij.j2meplugin.module.settings.ui.MobileBuildSettings;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleConfigurationEditor;
import com.intellij.openapi.module.ModuleType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.openapi.roots.ui.configuration.DefaultModuleConfigurationEditorFactory;
import com.intellij.openapi.roots.ui.configuration.ModuleConfigurationEditorProvider;
import com.intellij.openapi.roots.ui.configuration.ModuleConfigurationState;

public class J2MEModuleEditorsProvider implements ModuleConfigurationEditorProvider {


  @Override
  public ModuleConfigurationEditor[] createEditors(ModuleConfigurationState state) {
    final ModifiableRootModel rootModel = state.getRootModel();
    final Module module = rootModel.getModule();
    if (ModuleType.get(module) != J2MEModuleType.getInstance()) return ModuleConfigurationEditor.EMPTY;

    final Project project = state.getProject();
    final DefaultModuleConfigurationEditorFactory editorFactory = DefaultModuleConfigurationEditorFactory.getInstance();

    return new ModuleConfigurationEditor[]{
      editorFactory.createModuleContentRootsEditor(state),
      editorFactory.createOutputEditor(state),
      editorFactory.createClasspathEditor(state),
      new J2MEModuleConfEditor(module, project),
      new MobileBuildSettings(module, rootModel),
    };
  }

}
