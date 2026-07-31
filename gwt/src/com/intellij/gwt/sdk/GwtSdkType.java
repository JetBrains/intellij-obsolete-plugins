package com.intellij.gwt.sdk;

import com.intellij.openapi.extensions.ExtensionPointName;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;

public abstract class GwtSdkType {
  public static final ExtensionPointName<GwtSdkType> EP_NAME = ExtensionPointName.create("com.intellij.gwt.sdkType");
  private final @NonNls String myId;

  protected GwtSdkType(String id) {
    myId = id;
  }

  public final String getId() {
    return myId;
  }

  public abstract @NotNull GwtSdk createSdk(String homeDirectoryUrl);

  public abstract boolean isValidSdkHomeDirectory(File directory);

  public abstract boolean isEditable();

  public static @Nullable GwtSdkType findType(@Nullable String id) {
    for (GwtSdkType type : EP_NAME.getExtensions()) {
      if (type.getId().equals(id)) {
        return type;
      }
    }
    return null;
  }

  public abstract String getPresentableName(String sdkPath);
}
