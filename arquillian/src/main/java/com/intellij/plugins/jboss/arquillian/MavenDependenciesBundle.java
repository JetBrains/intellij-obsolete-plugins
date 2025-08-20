package com.intellij.plugins.jboss.arquillian;

import com.intellij.DynamicBundle;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.PropertyKey;

import java.util.function.Supplier;

public final class MavenDependenciesBundle extends DynamicBundle {

  @NonNls
  private static final String PATH_TO_BUNDLE = "messages.MavenDependenciesBundle";
  private static final MavenDependenciesBundle ourInstance = new MavenDependenciesBundle();

  private MavenDependenciesBundle() {
    super(PATH_TO_BUNDLE);
  }

  public static @Nls String message(@NotNull @PropertyKey(resourceBundle = PATH_TO_BUNDLE) String key, Object @NotNull ... params) {
    return ourInstance.getMessage(key, params);
  }

  @NotNull
  public static Supplier<@Nls String> messagePointer(@NotNull @PropertyKey(resourceBundle = PATH_TO_BUNDLE) String key, Object @NotNull ... params) {
    return ourInstance.getLazyMessage(key, params);
  }
}