package com.intellij.lang.javascript.linter.jslint;

import com.intellij.json.JsonFileType;
import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.javascript.linter.*;
import com.intellij.openapi.fileTypes.FileTypeRegistry;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;

public class JSLintJsonExternalAnnotator extends JSLinterExternalAnnotator<JSLintState> {

  public JSLintJsonExternalAnnotator() {
    super(true);
  }

  @NotNull
  @Override
  protected JSLinterConfigurable<JSLintState> createSettingsConfigurable(@NotNull Project project) {
    return new JSLintConfigurable(project);
  }

  @Override
  protected Class<? extends JSLinterConfiguration<JSLintState>> getConfigurationClass() {
    return JSLintConfiguration.class;
  }

  @Override
  protected Class<? extends JSLinterInspection> getInspectionClass() {
    return JSLintInspection.class;
  }

  @Override
  protected boolean acceptPsiFile(@NotNull PsiFile file) {
    VirtualFile vf = file.getVirtualFile();
    if (vf != null) {
      return FileTypeRegistry.getInstance().isFileOfType(vf, JsonFileType.INSTANCE);
    }
    return false;
  }

  @Override
  protected boolean acceptState(@NotNull JSLintState state) {
    return state.isValidateJson();
  }

  @Override
  public JSLinterAnnotationResult doAnnotate(JSLinterInput<JSLintState> collectedInfo) {
    return JSLintExternalAnnotator.getInstanceForBatchInspection().doAnnotate(collectedInfo);
  }

  @Override
  public void apply(@NotNull PsiFile file, JSLinterAnnotationResult annotationResult, @NotNull AnnotationHolder holder) {
    JSLintExternalAnnotator.getInstanceForBatchInspection().apply(file, annotationResult, holder);
  }
}
