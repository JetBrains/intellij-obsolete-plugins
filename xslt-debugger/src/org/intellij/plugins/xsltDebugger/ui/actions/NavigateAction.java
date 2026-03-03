// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package org.intellij.plugins.xsltDebugger.ui.actions;

import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.IdeActions;

public final class NavigateAction {
  private NavigateAction() {
  }

  public static AnAction getInstance() {
    return ActionManager.getInstance().getAction(IdeActions.ACTION_EDIT_SOURCE);
  }
}