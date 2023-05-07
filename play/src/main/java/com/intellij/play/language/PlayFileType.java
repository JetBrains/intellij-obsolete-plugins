package com.intellij.play.language;

import com.intellij.openapi.fileTypes.LanguageFileType;
import com.intellij.play.PlayIcons;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public final class PlayFileType extends LanguageFileType {
  public static final PlayFileType INSTANCE = new PlayFileType();

  private PlayFileType() {
    super(PlayLanguage.INSTANCE);
  }

  @Override
  @NotNull
  public String getName() {
    return "Play";
  }

  @Override
  @NotNull @NonNls
  public String getDescription() {
    return "Play";
  }

  @Override
  @NotNull
  public String getDefaultExtension() {
    return "play";
  }

  @Override
  public Icon getIcon() {
    return PlayIcons.Play;
  }
}

