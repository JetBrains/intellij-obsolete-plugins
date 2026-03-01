/*
 * Copyright (c) 2000-2005 by JetBrains s.r.o. All Rights Reserved.
 * Use is subject to license terms.
 */
package com.intellij.play.language;

import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

public class PlayTokenType extends IElementType {

  public PlayTokenType(@NotNull @NonNls final String debugName) {
    super(debugName, PlayLanguage.INSTANCE);
  }

}
