package com.intellij.seam.pages.xml.pages;

import org.jetbrains.annotations.NotNull;

public interface RenderOwner extends SeamPagesDomElement {
  @NotNull
  Render getRender();
}
