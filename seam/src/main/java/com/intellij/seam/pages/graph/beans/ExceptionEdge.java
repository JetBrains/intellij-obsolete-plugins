package com.intellij.seam.pages.graph.beans;

import com.intellij.seam.pages.xml.pages.PagesException;
import com.intellij.seam.pages.xml.pages.Redirect;
import org.jetbrains.annotations.NotNull;

public class ExceptionEdge extends BasicPagesEdge<PagesException> {
  public ExceptionEdge(@NotNull final BasicPagesNode source, @NotNull final BasicPagesNode target, final Redirect redirect, @NotNull final PagesException parentElement) {
    super(source, target, redirect, parentElement);
  }
}
