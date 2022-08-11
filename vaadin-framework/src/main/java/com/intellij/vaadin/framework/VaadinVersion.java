package com.intellij.vaadin.framework;

import com.intellij.openapi.util.NlsSafe;
import com.intellij.vaadin.templates.VaadinTemplateNames;
import org.jetbrains.annotations.NotNull;

public interface VaadinVersion {
  @NotNull
  VaadinTemplateNames getTemplateNames();

  @NotNull
  @NlsSafe String getVersionName();

  @NotNull
  String getApplicationServletParameterName();

  @NotNull
  String getWidgetSetModuleName();

  boolean isFullDistributionRequired();

  @NotNull
  String getServletClass();
}
