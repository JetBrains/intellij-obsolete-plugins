package com.intellij.play.language;

import com.intellij.lang.properties.PropertiesFileType;
import com.intellij.lang.properties.PropertiesLanguage;
import com.intellij.openapi.fileTypes.LanguageFileType;
import com.intellij.openapi.fileTypes.ex.FileTypeIdentifiableByVirtualFile;
import com.intellij.openapi.util.NotNullLazyValue;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.Locale;
import java.util.Set;

public final class PlayMessagesFileType  extends LanguageFileType implements FileTypeIdentifiableByVirtualFile {
  public static final PlayMessagesFileType INSTANCE = new PlayMessagesFileType();

  private static final String FILE_NAME = "messages";

  private static final NotNullLazyValue<Set<String>> LANGUAGE_FILENAMES = NotNullLazyValue.lazy(
    () -> ContainerUtil.map2Set(Locale.getAvailableLocales(),
                                locale -> FILE_NAME + "." + locale.getLanguage()));

  private PlayMessagesFileType() {
    super(PropertiesLanguage.INSTANCE, true);
  }

  @Override
  public boolean isMyFileType(@NotNull VirtualFile file) {
    String fileName = file.getName();
    return fileName.equals(FILE_NAME)  || LANGUAGE_FILENAMES.getValue().contains(fileName);
  }

  @NotNull
  @Override
  public String getName() {
    return "PlayMessages";
  }

  @NotNull
  @Override @NonNls
  public String getDescription() {
    return "Play Messages";
  }
  @NotNull
  @Override @NonNls
  public String getDisplayName() {
    return "Play Messages";
  }

  @NotNull
  @Override
  public String getDefaultExtension() {
    return "";
  }

  @Override
  public Icon getIcon() {
    return PropertiesFileType.INSTANCE.getIcon();
  }

  @Override
  public String getCharset(@NotNull VirtualFile file, byte @NotNull [] content) {
    return PropertiesFileType.INSTANCE.getCharset(file, content);
  }
}