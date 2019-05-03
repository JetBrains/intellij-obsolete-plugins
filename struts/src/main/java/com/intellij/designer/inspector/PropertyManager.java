/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.intellij.designer.inspector;

/**
 * @author spleaner
 */
public interface PropertyManager<P extends Property> {

  boolean canCreateProperties();
  P createProperty(Property parent);

  boolean isRemovable(P property);
  void removeProperty(P property);

}
