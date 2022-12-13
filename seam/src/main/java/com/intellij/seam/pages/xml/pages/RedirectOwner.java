package com.intellij.seam.pages.xml.pages;

import org.jetbrains.annotations.NotNull;

public interface RedirectOwner extends SeamPagesDomElement {
  @NotNull
  Redirect getRedirect();
}
