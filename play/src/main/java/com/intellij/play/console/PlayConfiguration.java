package com.intellij.play.console;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.util.xmlb.annotations.Attribute;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


@State(name = "PlayframeworkConfiguration", storages = @Storage("other.xml"))
public class PlayConfiguration implements PersistentStateComponent<PlayConfiguration> {

  public static PlayConfiguration getConfiguration() {
    return ApplicationManager.getApplication().getService(PlayConfiguration.class);
  }

  @Attribute("home")
  @Nullable
  public String myPlayHome;

  @Attribute("path")
  @Nullable
  public String myPath;

  @Attribute("show")
  @Nullable
  public Boolean myShowOnRun;

  @Override
  public PlayConfiguration getState() {
    return this;
  }

  @Override
  public void loadState(@NotNull PlayConfiguration state) {
    myPlayHome = state.myPlayHome;
    myPath = state.myPath;
    myShowOnRun = state.myShowOnRun;
  }
}
