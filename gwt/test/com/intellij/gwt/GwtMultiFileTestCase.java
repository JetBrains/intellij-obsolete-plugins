package com.intellij.gwt;

import com.intellij.refactoring.MultiFileTestCase;
import org.jetbrains.annotations.NotNull;

public abstract class GwtMultiFileTestCase extends MultiFileTestCase {

  @NotNull
  @Override
  protected String getTestDataPath() {
    return GwtTestCase.getGwtTestDataPath();
  }
}
