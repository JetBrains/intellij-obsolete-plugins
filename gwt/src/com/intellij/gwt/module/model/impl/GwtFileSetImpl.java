package com.intellij.gwt.module.model.impl;

import com.intellij.gwt.module.model.GwtFileSet;
import org.jetbrains.annotations.NotNull;

import java.util.regex.Pattern;

public abstract class GwtFileSetImpl implements GwtFileSet {
  private Pattern myPattern;

  @Override
  public boolean matches(@NotNull String path, boolean caseSensitive) {
    if (myPattern == null) {
      final String value = getName().getValue();
      if (value != null) {
        myPattern = GwtFilePatternUtil.createPattern(value, caseSensitive);
      }
    }
    return myPattern.matcher(path).matches();
  }
}
