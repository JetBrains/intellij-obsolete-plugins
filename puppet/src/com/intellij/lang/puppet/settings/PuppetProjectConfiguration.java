package com.intellij.lang.puppet.settings;

import com.intellij.lang.puppet.PuppetLanguage;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.NlsSafe;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.util.xmlb.XmlSerializerUtil;
import com.intellij.util.xmlb.annotations.Attribute;
import com.intellij.util.xmlb.annotations.Tag;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.intellij.lang.puppet.adapters.PuppetLibrarianAdapter.LIBRARIAN_EXECUTABLE_NAME;
import static com.intellij.lang.puppet.adapters.PuppetLibrarianAdapter.LIBRARIAN_EXECUTABLE_WIN_NAME;

@State(name = "PuppetProjectConfiguration")
public class PuppetProjectConfiguration implements PersistentStateComponent<PuppetProjectConfiguration.State> {

  private final State myState = new State();

  public @NotNull PuppetLanguage.Version getLanguageVersion() {
    return myState.myLanguageVersion;
  }

  public void setLanguageVersion(@NotNull PuppetLanguage.Version languageVersion) {
    myState.myLanguageVersion = languageVersion;
  }

  public @NotNull @NlsSafe String getLibrarianPath() {return myState.myLibrarianPath;}

  public void setLibrarianPath(@NotNull String newLibrarianPath) {myState.myLibrarianPath = newLibrarianPath;}

  @Override
  public @Nullable State getState() {
    return myState;
  }

  @Override
  public void loadState(@NotNull State state) {
    XmlSerializerUtil.copyBean(state, myState);
  }

  public static PuppetProjectConfiguration getInstance(@NotNull Project project) {
    return project.getService(PuppetProjectConfiguration.class);
  }

  public static final class State {
    @Attribute("languageVersion") private @NotNull PuppetLanguage.Version myLanguageVersion = PuppetLanguage.Version.PUPPET_4;

    @Tag("librarianPath") private @NotNull @NlsSafe String myLibrarianPath =
      SystemInfo.isWindows ? LIBRARIAN_EXECUTABLE_WIN_NAME : LIBRARIAN_EXECUTABLE_NAME;

    private State() {
    }
  }
}
