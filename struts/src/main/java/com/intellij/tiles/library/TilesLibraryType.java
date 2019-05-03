/*
 * Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.intellij.tiles.library;

import com.intellij.framework.library.DownloadableLibraryType;
import com.intellij.tiles.TilesConstants;
import icons.StrutsApiIcons;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public class TilesLibraryType extends DownloadableLibraryType {

  public TilesLibraryType() {
    super("Tiles", "tiles", "tiles",
          TilesLibraryType.class.getResource("/libraries/tiles.xml"));
  }

  @NotNull
  @Override
  public Icon getLibraryTypeIcon() {
    return StrutsApiIcons.Tiles.Tile;
  }

  @NotNull
  @Override
  protected String[] getDetectionClassNames() {
    return new String[]{TilesConstants.DEFINITION};
  }
}
