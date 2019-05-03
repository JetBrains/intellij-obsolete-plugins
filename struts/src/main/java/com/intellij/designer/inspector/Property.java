/*
 * Copyright (c) 2005 JetBrains s.r.o. All Rights Reserved.
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
