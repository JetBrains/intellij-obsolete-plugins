package com.intellij.seam.dependencies.beans;

import com.intellij.jam.model.common.CommonModelElement;
import org.jetbrains.annotations.NotNull;

public interface  SeamDependencyInfo<T extends CommonModelElement> {
  SeamComponentNodeInfo getSource();

  SeamComponentNodeInfo getTarget();

  String getName();

  @NotNull
  T getIdentifyingElement();
}
