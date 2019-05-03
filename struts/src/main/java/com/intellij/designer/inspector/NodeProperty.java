/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.intellij.designer.inspector;

import com.intellij.openapi.actionSystem.AnActionEvent;

/**
 * @author spleaner
 */
public interface NodeProperty<P extends Property> {

  void createAndAddChildProperty(Property current, AnActionEvent e);

  void removeProperty(P property, AnActionEvent e);

  boolean isRemovable(Property property);
}
