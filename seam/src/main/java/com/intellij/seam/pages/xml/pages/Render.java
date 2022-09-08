package com.intellij.seam.pages.xml.pages;


import org.jetbrains.annotations.NotNull;

public interface Render extends PagesViewIdOwner, SeamPagesDomElement {

  @NotNull
  Message getMessage();
}
