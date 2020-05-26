package com.intellij.lang.javascript.linter.jslint;

import com.intellij.lang.javascript.linter.JSLinterInspection;
import org.jetbrains.annotations.NotNull;

public class JSLintInspection extends JSLinterInspection {

  @NotNull
  @Override
  protected JSLintExternalAnnotator getExternalAnnotatorForBatchInspection() {
    return JSLintExternalAnnotator.getInstanceForBatchInspection();
  }
}
