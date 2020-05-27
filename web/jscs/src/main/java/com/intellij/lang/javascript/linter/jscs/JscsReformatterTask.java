package com.intellij.lang.javascript.linter.jscs;

import com.intellij.lang.javascript.linter.JSLinterAnnotationResult;
import com.intellij.lang.javascript.linter.JSLinterError;
import com.intellij.lang.javascript.linter.JSLinterInput;
import com.intellij.lang.javascript.linter.eslint.JSLinterReformatterTask;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;

/**
 * @author Irina.Chernushina on 4/16/2015.
 */
public class JscsReformatterTask extends JSLinterReformatterTask {
  private final JscsExternalAnnotator myJscsAnnotator;
  private final JscsState myJscsState;

  public JscsReformatterTask(@NotNull Project project, @NotNull final Collection<? extends VirtualFile> roots, @NotNull Runnable completionCallback) {
    super(project, JscsBundle.message("settings.javascript.linters.jscs.configurable.name"), roots, completionCallback);
    myJscsAnnotator = JscsExternalAnnotator.getInstanceForBatchInspection();
    myJscsState = JscsConfiguration.getInstance(project).getExtendedState().getState();
  }

  @Override
  protected void runLinter(@NotNull final PsiFile psiFile, @NotNull final Document document) {
    final VirtualFile file = psiFile.getVirtualFile();
    final JSLinterInput<JscsState> input =
      JSLinterInput.create(psiFile, myJscsState, null);
    final JscsExternalRunner runner = myJscsAnnotator.createJscsRunner(input);
    runner.setFix(true);
    final JSLinterAnnotationResult result = runner.execute();
    if (result != null) {
      if (result.getFileLevelError() != null) {
        error(file, result.getFileLevelError().getMessage());
      }
      final List<JSLinterError> errors = result.getErrors();
      for (JSLinterError error : errors) {
        error(file, error.getDescription());
      }
    }
    if (runner.getSkipReason() != null) {
      error(file, runner.getSkipReason());
    }
  }
}
