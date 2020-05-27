package com.intellij.lang.javascript.linter.jscs.config;

import com.intellij.lang.javascript.linter.JSLinterErrorBase;
import org.jetbrains.annotations.NotNull;

/**
 * @author Irina.Chernushina on 10/21/2014.
 *
 */
public class JscsTypeError extends JSLinterErrorBase {
  public JscsTypeError(@NotNull String description) {
    super(description);
  }
}
