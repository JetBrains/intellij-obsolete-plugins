package com.intellij.play.language;

import com.intellij.lang.Language;
import com.intellij.openapi.fileTypes.LanguageFileType;
import com.intellij.psi.templateLanguages.TemplateLanguage;

public final class PlayLanguage extends Language implements TemplateLanguage {
  public static PlayLanguage INSTANCE = new PlayLanguage();

  private PlayLanguage() {
    super("Play", "");
  }

  @Override
  public LanguageFileType getAssociatedFileType() {
    return PlayFileType.INSTANCE;
  }
}
