/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */
package com.intellij.designer.inspector;

import com.intellij.openapi.util.UserDataHolder;

import javax.swing.tree.TreeNode;

/**
 * @author spleaner
 */
public interface Property<N, V> extends TreeNode, UserDataHolder {

  N getName();
  V getValue();

  void accept(PropertyVisitor visitor);

  Property getParentProperty();

  boolean isValid();
}
