/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.intellij.struts;

import com.intellij.util.xml.DomElement;
import com.intellij.util.xml.model.DomModel;
import org.jetbrains.annotations.NotNull;

/**
 * @author Dmitry Avdeev
 */
public interface NamedDomModel<T extends DomElement> extends DomModel<T> {
  @NotNull
  String getName();
}
