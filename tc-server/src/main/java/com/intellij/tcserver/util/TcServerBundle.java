package com.intellij.tcserver.util;

import com.intellij.DynamicBundle;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.PropertyKey;

import java.sql.Date;
import java.text.SimpleDateFormat;
import java.util.function.Supplier;

public final class TcServerBundle {
  private static final @NonNls String BUNDLE = "messages.TcServerBundle";
  private static final DynamicBundle INSTANCE = new DynamicBundle(TcServerBundle.class, BUNDLE);

  private TcServerBundle() {}

  public static @NotNull @Nls String message(@NotNull @PropertyKey(resourceBundle = BUNDLE) String key, Object @NotNull ... params) {
    return INSTANCE.getMessage(key, params);
  }

  public static @NotNull Supplier<@Nls String> messagePointer(@NotNull @PropertyKey(resourceBundle = BUNDLE) String key, Object @NotNull ... params) {
    return INSTANCE.getLazyMessage(key, params);
  }

  private static final @NonNls SimpleDateFormat MY_DATE_FORMAT = new SimpleDateFormat("[HH:mm:ss.SSS]");

  public static String datedMessage(@PropertyKey(resourceBundle = BUNDLE) String key, Object... params) {
    return MY_DATE_FORMAT.format(new Date(System.currentTimeMillis())) + " " + message(key, params);
  }
}
