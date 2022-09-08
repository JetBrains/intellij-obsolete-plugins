package com.intellij.seam.dependencies.beans;

import com.intellij.jam.model.common.CommonModelElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public interface  SeamComponentNodeInfo<T extends CommonModelElement> {
  @Nullable
  String getName();

  Icon getIcon();

  @NotNull
  T getIdentifyingElement();
}
