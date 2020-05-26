package com.intellij.lang.javascript.linter.gjslint;

import com.intellij.lang.javascript.linter.JSLinterInspection;
import org.jetbrains.annotations.NotNull;

public class GjsLintInspection extends JSLinterInspection {

  @NotNull
  @Override
  protected GjsLintExternalAnnotator getExternalAnnotatorForBatchInspection() {
    return GjsLintExternalAnnotator.getInstanceForBatchInspection();
  }
}
