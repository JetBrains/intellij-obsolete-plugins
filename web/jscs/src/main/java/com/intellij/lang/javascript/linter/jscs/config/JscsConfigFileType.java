package com.intellij.lang.javascript.linter.jscs.config;

import com.intellij.icons.AllIcons;
import com.intellij.json.JsonFileType;
import com.intellij.json.JsonLanguage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

/**
 * @author by Irina.Chernushina on 9/24/2014.
 */
public class JscsConfigFileType extends JsonFileType {
  public static final JscsConfigFileType INSTANCE = new JscsConfigFileType();

  protected JscsConfigFileType() {
    super(JsonLanguage.INSTANCE, true);
  }

  @NotNull
  @Override
  public String getName() {
    return "JSCS";
  }

  @NotNull
  @Override
  public String getDescription() {
    return "JSCS configuration";
  }

  @NotNull
  @Override
  public String getDefaultExtension() {
    return "jscsrc;jscs.json";
  }

  @Nullable
  @Override
  public Icon getIcon() {
    return AllIcons.FileTypes.Json;
  }
}
