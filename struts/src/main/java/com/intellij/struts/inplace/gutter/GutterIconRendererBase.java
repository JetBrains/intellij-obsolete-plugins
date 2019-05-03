/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.intellij.struts.inplace.gutter;

import com.intellij.openapi.editor.markup.GutterIconRenderer;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

/**
 * @author davdeev
 */
public abstract class GutterIconRendererBase extends GutterIconRenderer {

  @NotNull
  private final Icon myIcon;

  protected GutterIconRendererBase(@NotNull final Icon icon) {
    myIcon = icon;
  }

  /**
   * Returns the icon drawn in the gutter.
   *
   * @return the gutter icon.
   */
  @Override
  @NotNull
  public Icon getIcon() {
    return myIcon;
  }

}