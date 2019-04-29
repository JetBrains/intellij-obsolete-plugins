/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */
package com.intellij.j2meplugin.emulator.ui;

import com.intellij.j2meplugin.emulator.Emulator;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SettingsEditor;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public abstract class MobileApiSettingsEditor extends SettingsEditor<Emulator> {
  protected boolean myModified = false;

  @Override
  @NotNull
  public abstract JComponent createEditor();

  @Override
  public abstract void resetEditorFrom(@NotNull Emulator s);

  @Override
  public abstract void applyEditorTo(@NotNull Emulator s) throws ConfigurationException;


  public boolean isModified() {
    return myModified;
  }

}
