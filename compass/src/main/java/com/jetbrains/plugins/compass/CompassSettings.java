package com.jetbrains.plugins.compass;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleServiceManager;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.ThreeState;
import com.intellij.util.xmlb.XmlSerializerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

@State(name = "CompassSettings")
public class CompassSettings implements PersistentStateComponent<CompassSettings> {
  @NotNull private Module myModule;
  @NotNull private ThreeState myCompassSupportEnabled = ThreeState.UNSURE;
  @Nullable private String myCompassExecutableFilePath = null;
  @Nullable private String myCompassConfigPath = null;
  @NotNull private List<String> myImportPaths = new ArrayList<>();

  public static CompassSettings getInstance(@NotNull Module module) {
    return !module.isDisposed() ? ModuleServiceManager.getService(module, CompassSettings.class) : null;
  }

  @Override
  public void loadState(@NotNull CompassSettings state) {
    XmlSerializerUtil.copyBean(state, this);
  }

  @SuppressWarnings("UnusedDeclaration")
  // for serialization only
  public CompassSettings() {
  }

  @SuppressWarnings("UnusedDeclaration")
  public CompassSettings(@NotNull Module module) {
    myModule = module;
  }

  public boolean isCompassSupportEnabled() {
    //noinspection ConstantConditions
    return myCompassSupportEnabled == ThreeState.YES ||
           myCompassSupportEnabled == ThreeState.UNSURE && myModule != null
           && CompassUtil.isGemInstalled(CompassUtil.COMPASS_GEM_NAME, myModule);
  }

  public void setCompassSupportEnabled(boolean compassSupportEnabled) {
    myCompassSupportEnabled = compassSupportEnabled ? ThreeState.YES : ThreeState.NO;
  }

  public void resetEnabledFlag() {
    myCompassSupportEnabled = ThreeState.UNSURE;
  }

  @NotNull
  public String getCompassExecutableFilePath() {
    return StringUtil.notNullize(myCompassExecutableFilePath);
  }

  public void setCompassExecutableFilePath(@Nullable final String compassExecutablePath) {
    myCompassExecutableFilePath = compassExecutablePath;
  }

  @NotNull
  public String getCompassConfigPath() {
    return StringUtil.notNullize(myCompassConfigPath);
  }

  public void setCompassConfigPath(@Nullable String compassConfigPath) {
    myCompassConfigPath = compassConfigPath;
  }

  @Nullable
  @Override
  public CompassSettings getState() {
    return this;
  }

  @NotNull
  public Module getModule() {
    return myModule;
  }

  @NotNull
  public List<String> getImportPaths() {
    return myImportPaths;
  }

  public void setImportPaths(@NotNull List<String> importPaths) {
    myImportPaths = importPaths;
  }
}
