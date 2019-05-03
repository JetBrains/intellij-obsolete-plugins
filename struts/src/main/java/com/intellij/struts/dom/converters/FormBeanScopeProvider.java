
/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.intellij.struts.dom.converters;

import com.intellij.struts.dom.StrutsConfig;
import com.intellij.util.xml.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Dmitry Avdeev
 */
public class FormBeanScopeProvider extends ScopeProvider {

  @Override
  @Nullable
  public DomElement getScope(@NotNull final DomElement element) {
    final DomFileElement<StrutsConfig> root = DomUtil.getFileElement(element);
    final MergingFileDescription<StrutsConfig> description = (MergingFileDescription)root.getFileDescription();
    final StrutsConfig config = description.getMergedRoot(root);
    return config.getFormBeans();
  }
}
