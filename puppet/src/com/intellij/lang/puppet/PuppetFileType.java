package com.intellij.lang.puppet;

import com.intellij.lang.Language;
import com.intellij.openapi.fileTypes.LanguageFileType;
import org.jetbrains.annotations.NotNull;

import javax.swing.Icon;

public class PuppetFileType extends LanguageFileType {
  public static final PuppetFileType INSTANCE = new PuppetFileType();
  public static final String NAME = "Puppet";
  public static final String DEFAULT_EXTENSION = "pp";

  private PuppetFileType() {
    super(PuppetLanguage.INSTANCE);
  }

  protected PuppetFileType(@NotNull Language language) {
    super(language);
  }

  @Override
  public @NotNull String getName() {
    return NAME;
  }

  @Override
  public @NotNull String getDescription() {
    return PuppetBundle.message("filetype.puppet.description");
  }

  @Override
  public @NotNull String getDefaultExtension() {
    return DEFAULT_EXTENSION;
  }

  @Override
  public Icon getIcon() {
    return PuppetIcons.PuppetLogo;
  }
}
