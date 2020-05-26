package com.intellij.lang.javascript.linter.gjslint;

import com.intellij.lang.javascript.linter.JSLinterState;
import org.jetbrains.annotations.NotNull;

/**
 * @author Sergey Simonchik
 */
public class GjsLintState implements JSLinterState {

  private final String myLinterExePath;
  private final String myConfigFilePath;

  private GjsLintState(@NotNull String linterExePath,
                       @NotNull String configFilePath) {
    myLinterExePath = linterExePath;
    myConfigFilePath = configFilePath;
  }

  @NotNull
  public String getLinterExePath() {
    return myLinterExePath;
  }

  @NotNull
  public String getConfigFilePath() {
    return myConfigFilePath;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    GjsLintState state = (GjsLintState)o;

    return myConfigFilePath.equals(state.myConfigFilePath) &&
           myLinterExePath.equals(state.myLinterExePath);
  }

  @Override
  public int hashCode() {
    int result = myLinterExePath.hashCode();
    result = 31 * result + myConfigFilePath.hashCode();
    return result;
  }

  public static class Builder {
    private String myLinterExePath = "";
    private String myConfigFilePath = "";

    @NotNull
    public Builder setLinterExePath(@NotNull String linterExePath) {
      myLinterExePath = linterExePath;
      return this;
    }

    @NotNull
    public Builder setConfigFilePath(@NotNull String configFilePath) {
      myConfigFilePath = configFilePath;
      return this;
    }

    @NotNull
    public GjsLintState build() {
      return new GjsLintState(myLinterExePath, myConfigFilePath);
    }
  }
}
