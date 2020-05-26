package com.intellij.lang.javascript.linter.gjslint;

import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.javascript.linter.*;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class GjsLintHtmlExternalAnnotator extends JSLinterExternalAnnotator<GjsLintState> {

  public GjsLintHtmlExternalAnnotator() {
    super(true);
  }

  @NotNull
  @Override
  protected JSLinterConfigurable<GjsLintState> createSettingsConfigurable(@NotNull Project project) {
    return new GjsLintConfigurable(project, true);
  }

  @Override
  protected Class<? extends JSLinterConfiguration<GjsLintState>> getConfigurationClass() {
    return GjsLintConfiguration.class;
  }

  @Override
  protected Class<? extends JSLinterInspection> getInspectionClass() {
    return GjsLintInspection.class;
  }

  @Override
  protected boolean acceptPsiFile(@NotNull PsiFile file) {
    return JSLinterUtil.isPureHtmlFile(file);
  }

  @Nullable
  @Override
  public JSLinterAnnotationResult doAnnotate(@Nullable JSLinterInput<GjsLintState> collectedInfo) {
    return GjsLintExternalAnnotator.getInstanceForBatchInspection().doAnnotate(collectedInfo);
  }

  @Override
  public void apply(@NotNull PsiFile file,
                    @Nullable JSLinterAnnotationResult annotationResult,
                    @NotNull AnnotationHolder holder) {
    GjsLintExternalAnnotator.getInstanceForBatchInspection().apply(file, annotationResult, holder);
  }
}
