// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.lang.gsp.parsing.composite;

import com.intellij.psi.tree.ICompositeElementType;
import com.intellij.psi.tree.xml.IXmlElementType;

public abstract class GspCompositeElementType extends IXmlElementType implements ICompositeElementType {
  public GspCompositeElementType(String debugName) {
    super(debugName);
  }
}
