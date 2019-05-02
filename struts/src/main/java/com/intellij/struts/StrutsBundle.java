package com.intellij.struts;

import com.intellij.AbstractBundle;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.PropertyKey;

/**
 * @author Dmitry Avdeev
 */
public class StrutsBundle extends AbstractBundle {

  public static String message(@NotNull @PropertyKey(resourceBundle = PATH_TO_BUNDLE) String key, @NotNull Object... params) {
    return ourInstance.getMessage(key, params);
  }

  @NonNls
  private static final String PATH_TO_BUNDLE = "StrutsBundle";
  private static final StrutsBundle ourInstance = new StrutsBundle();

  private StrutsBundle() {
    super(PATH_TO_BUNDLE);
  }
}