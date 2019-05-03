/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.intellij.struts.inplace.generate;

import com.intellij.openapi.actionSystem.DefaultActionGroup;

/**
 * @author Dmitry Avdeev
 */
public class StrutsGenerateActionsGroup extends DefaultActionGroup {

  public StrutsGenerateActionsGroup() {
    add(new GenerateActionMappingAction());
    add(new GenerateFormAction());
    add(new GenerateForwardAction());
    add(new GenerateGlobalForwardAction());
  }
}
