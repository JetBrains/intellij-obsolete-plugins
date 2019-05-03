/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.intellij.struts.highlighting;

import com.intellij.struts.dom.tiles.TilesDefinitions;
import com.intellij.struts.inplace.InplaceUtil;
import com.intellij.util.xml.GenericDomValue;
import com.intellij.util.xml.highlighting.BasicDomElementsInspection;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

public class TilesInspection extends BasicDomElementsInspection<TilesDefinitions> {

  public TilesInspection() {
    super(TilesDefinitions.class);
  }


  @Override
  protected boolean shouldCheckResolveProblems(GenericDomValue value) {
    final String text = value.getRawText();
    if (InplaceUtil.containsPlaceholderReference(text)) {
      return false;
    }
    return super.shouldCheckResolveProblems(value);
  }

  @Override
  @NotNull
  @NonNls
  public String getShortName() {
    return "StrutsTilesInspection";
  }
}
