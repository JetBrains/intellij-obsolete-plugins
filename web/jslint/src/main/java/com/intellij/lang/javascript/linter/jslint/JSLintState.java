package com.intellij.lang.javascript.linter.jslint;

import com.intellij.lang.javascript.linter.JSLinterState;
import org.jetbrains.annotations.NotNull;

/**
 * Immutable state of JSLint:
 * <ul>
 *   <li>enable/disable</li>
 *   <li>option's value list</li>
 * </ul>.
 * 
 * @author Sergey Simonchik
 */
public class JSLintState implements JSLinterState {

  private final JSLintOptionsState myOptionsState;
  private final boolean myValidateJson;

  private JSLintState(@NotNull JSLintOptionsState optionsState,
                      boolean validateJson) {
    myOptionsState = optionsState;
    myValidateJson = validateJson;
  }

  @NotNull
  public JSLintOptionsState getOptionsState() {
    return myOptionsState;
  }

  public boolean isValidateJson() {
    return myValidateJson;
  }

  @Override
  public String toString() {
    return "JSLint options: " + myOptionsState;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    JSLintState state = (JSLintState)o;

    if (myValidateJson != state.myValidateJson) return false;
    if (!myOptionsState.equals(state.myOptionsState)) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = myOptionsState.hashCode();
    result = 31 * result + (myValidateJson ? 1 : 0);
    return result;
  }

  public static class Builder {
    private JSLintOptionsState myOptionsState;
    private boolean myValidateJson;

    public Builder setOptionsState(@NotNull JSLintOptionsState optionsState) {
      myOptionsState = optionsState;
      return this;
    }

    @NotNull
    public Builder setValidateJson(boolean validateJson) {
      myValidateJson = validateJson;
      return this;
    }

    @NotNull
    public JSLintState build() {
      return new JSLintState(myOptionsState, myValidateJson);
    }
  }

}
