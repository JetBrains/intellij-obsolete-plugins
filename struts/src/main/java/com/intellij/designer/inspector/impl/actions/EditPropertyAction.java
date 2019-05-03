/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.intellij.designer.inspector.impl.actions;

import com.intellij.designer.inspector.AbstractInspectorAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import org.jetbrains.annotations.NotNull;

/**
 * @author spleaner
 */
public class EditPropertyAction extends AbstractInspectorAction {

  public EditPropertyAction() {
    super("Edit property value");
  }

  @Override
  public void actionPerformed(@NotNull AnActionEvent e) {
    getInspector(e).processEnter();
  }
}
