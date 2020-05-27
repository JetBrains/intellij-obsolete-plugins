package com.intellij.lang.javascript.linter.jscs;

import com.intellij.lang.javascript.linter.JSLinterErrorBase;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * @author Irina.Chernushina on 10/13/2014.
 */
public class JscsObsoleteRulesWarning extends JSLinterErrorBase {
  public JscsObsoleteRulesWarning(@NotNull String description, @Nullable List<String> codes) {
    super(description, codes == null || codes.isEmpty() ? null : codes.get(0));
  }
}
