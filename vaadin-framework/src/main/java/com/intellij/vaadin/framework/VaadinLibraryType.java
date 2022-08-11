package com.intellij.vaadin.framework;

import com.intellij.framework.library.DownloadableLibraryType;
import com.intellij.vaadin.VaadinBundle;
import com.intellij.vaadin.VaadinIcons;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public class VaadinLibraryType extends DownloadableLibraryType {
  public static final String VAADIN_APPLICATION_CLASS = "com.vaadin.Application";

  public VaadinLibraryType() {
    super(VaadinBundle.VAADIN, "vaadin", "vaadin", VaadinLibraryType.class.getResource("/resources/library/vaadin.xml"));
  }

  @NotNull
  @Override
  public Icon getLibraryTypeIcon() {
    return VaadinIcons.VaadinIcon;
  }

  @Override
  protected String @NotNull [] getDetectionClassNames() {
    return new String[]{VAADIN_APPLICATION_CLASS};
  }

  public static VaadinLibraryType getInstance() {
    return EP_NAME.findExtension(VaadinLibraryType.class);
  }
}
