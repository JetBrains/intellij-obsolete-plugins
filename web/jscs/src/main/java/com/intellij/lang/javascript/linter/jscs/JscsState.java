package com.intellij.lang.javascript.linter.jscs;

import com.intellij.javascript.nodejs.interpreter.NodeJsInterpreterRef;
import com.intellij.javascript.nodejs.util.NodePackage;
import com.intellij.javascript.nodejs.util.NodePackageRef;
import com.intellij.lang.javascript.linter.JSNpmLinterState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * @author Irina.Chernushina on 9/22/2014.
 */
public class JscsState implements JSNpmLinterState<JscsState> {
  private final boolean myCustomConfigFileUsed;
  private String myCustomConfigFilePath;
  private final NodeJsInterpreterRef myInterpreterRef;
  private final String myPackagePath;
  @Nullable
  private final JscsPreset myPreset;

  private JscsState(boolean customConfigFileUsed,
                    String customConfigPath,
                    @NotNull NodeJsInterpreterRef nodePath,
                    @NotNull String packagePath,
                    @Nullable JscsPreset preset) {
    myCustomConfigFileUsed = customConfigFileUsed;
    myCustomConfigFilePath = customConfigPath;
    myInterpreterRef = nodePath;
    myPackagePath = packagePath;
    myPreset = preset;
  }

  public void setCustomConfigFilePath(String customConfigFilePath) {
    myCustomConfigFilePath = customConfigFilePath;
  }

  public boolean isCustomConfigFileUsed() {
    return myCustomConfigFileUsed;
  }

  public String getCustomConfigFilePath() {
    return myCustomConfigFilePath;
  }

  @Override
  @NotNull
  public NodeJsInterpreterRef getInterpreterRef() {
    return myInterpreterRef;
  }

  @NotNull
  public String getPackagePath() {
    return myPackagePath;
  }

  @Nullable
  public JscsPreset getPreset() {
    return myPreset;
  }

  @NotNull
  public NodePackage getNodePackage() {
    return new NodePackage(myPackagePath);
  }

  @NotNull
  @Override
  public NodePackageRef getNodePackageRef() {
    return NodePackageRef.create(getNodePackage());
  }

  @Override
  public JscsState withLinterPackage(@NotNull NodePackageRef nodePackage) {
    NodePackage constantPackage = nodePackage.getConstantPackage();
    assert constantPackage != null : this.getClass().getSimpleName() + " does not support non-constant package refs";
    return new JscsState(myCustomConfigFileUsed, myCustomConfigFilePath, myInterpreterRef, constantPackage.getSystemDependentPath(),
                         myPreset);
  }

  @Override
  public JscsState withInterpreterRef(@NotNull NodeJsInterpreterRef ref) {
    return new JscsState(myCustomConfigFileUsed, myCustomConfigFilePath, ref, myPackagePath, myPreset);
  }

  public static class Builder {
    private boolean myCustomConfigFileUsed = false;
    private String myCustomConfigFilePath = "";
    private NodeJsInterpreterRef myInterpreterRef = NodeJsInterpreterRef.createProjectRef();
    private String myPackagePath = "";
    @Nullable
    private JscsPreset myPreset;

    public Builder() {
    }

    public Builder(@NotNull JscsState state) {
      myCustomConfigFilePath = state.getCustomConfigFilePath();
      myCustomConfigFileUsed = state.isCustomConfigFileUsed();
      myInterpreterRef = state.getInterpreterRef();
      myPackagePath = state.getPackagePath();
      myPreset = state.getPreset();
    }

    public Builder setCustomConfigFileUsed(boolean customConfigFileUsed) {
      myCustomConfigFileUsed = customConfigFileUsed;
      return this;
    }

    public Builder setCustomConfigFilePath(@NotNull String customConfigFilePath) {
      myCustomConfigFilePath = customConfigFilePath;
      return this;
    }

    public Builder setNodePath(@NotNull final NodeJsInterpreterRef ref) {
      myInterpreterRef = ref;
      return this;
    }

    public Builder setNodePackage(@NotNull NodePackage packagePath) {
      myPackagePath = packagePath.getSystemDependentPath();
      return this;
    }

    public Builder setPreset(@Nullable JscsPreset preset) {
      myPreset = preset;
      return this;
    }

    public boolean isCustomConfigFileUsed() {
      return myCustomConfigFileUsed;
    }

    public String getCustomConfigFilePath() {
      return myCustomConfigFilePath;
    }

    public NodeJsInterpreterRef getInterpreterRef() {
      return myInterpreterRef;
    }

    public String getPackagePath() {
      return myPackagePath;
    }

    @Nullable
    public JscsPreset getPreset() {
      return myPreset;
    }

    public JscsState build() {
      return new JscsState(myCustomConfigFileUsed, myCustomConfigFilePath, myInterpreterRef, myPackagePath, myPreset);
    }
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    JscsState state = (JscsState)o;

    if (myCustomConfigFileUsed != state.myCustomConfigFileUsed) return false;
    if (!Objects.equals(myCustomConfigFilePath, state.myCustomConfigFilePath)) return false;
    if (!myInterpreterRef.equals(state.myInterpreterRef)) return false;
    if (!myPackagePath.equals(state.myPackagePath)) return false;
    if (myPreset != state.myPreset) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = (myCustomConfigFileUsed ? 1 : 0);
    result = 31 * result + (myCustomConfigFilePath != null ? myCustomConfigFilePath.hashCode() : 0);
    result = 31 * result + myInterpreterRef.hashCode();
    result = 31 * result + myPackagePath.hashCode();
    result = 31 * result + (myPreset != null ? myPreset.hashCode() : 0);
    return result;
  }

  @Override
  public String toString() {
    return "JscsState{" +
           "myCustomConfigFileUsed=" + myCustomConfigFileUsed +
           ", myCustomConfigFilePath='" + myCustomConfigFilePath + '\'' +
           ", myNodePath='" + myInterpreterRef + '\'' +
           ", myPackagePath='" + myPackagePath + '\'' +
           ", myPreset=" + myPreset +
           '}';
  }
}
