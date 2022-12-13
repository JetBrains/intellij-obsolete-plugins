package com.intellij.seam.pages.xml.pages;

import org.jetbrains.annotations.NotNull;

public interface TaskOwner extends SeamPagesDomElement {
  @NotNull
  StartTask getStartTask();

  @NotNull
  BeginTask getBeginTask();

  @NotNull
  EndTask getEndTask();
}
