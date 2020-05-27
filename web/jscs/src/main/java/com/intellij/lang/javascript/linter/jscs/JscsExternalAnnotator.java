package com.intellij.lang.javascript.linter.jscs;

import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.codeInsight.intention.impl.BaseIntentionAction;
import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.javascript.linter.*;
import com.intellij.lang.javascript.linter.jscs.config.*;
import com.intellij.lang.javascript.psi.JSFile;
import com.intellij.lang.javascript.psi.util.JSUtils;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author Irina.Chernushina on 9/22/2014.
 */
public class JscsExternalAnnotator extends JSLinterExternalAnnotator<JscsState> {
  private static final Logger LOG = Logger.getInstance(JscsConfiguration.LOG_CATEGORY);

  private static final JscsExternalAnnotator INSTANCE_FOR_BATCH_INSPECTION = new JscsExternalAnnotator(false);
  private static final String JSCS_CODE_TEMP_FILE_MAP_KEY_NAME = "JSCS_CODE_TEMP_FILE_MAP_KEY";
  private static final String JSCS_CONFIG_TEMP_FILE_MAP_KEY_NAME = "JSCS_CONFIG_TEMP_FILE_MAP_KEY";

  private final FilesMirror myCodeFilesMirror;
  private final FilesMirror myConfigFilesMirror;
  private final AtomicReference<File> myEmptyConfig;

  public static JscsExternalAnnotator getInstanceForBatchInspection() {
    return INSTANCE_FOR_BATCH_INSPECTION;
  }

  @SuppressWarnings("unused")
  public JscsExternalAnnotator() {
    this(true);
  }

  public JscsExternalAnnotator(boolean onTheFly) {
    super(onTheFly);
    myCodeFilesMirror = new FilesMirror(JSCS_CODE_TEMP_FILE_MAP_KEY_NAME, "jscs");
    myConfigFilesMirror = new FilesMirror(JSCS_CONFIG_TEMP_FILE_MAP_KEY_NAME, "jscs");
    myEmptyConfig = new AtomicReference<>();
  }

  private void tryCreateEmptyConfig() {
    if (myEmptyConfig.get() != null) return;
    try {
      final File empty = FileUtil.createTempFile("jscs_empty", ".jscsrc");
      FileUtil.writeToFile(empty, "{}");
      myEmptyConfig.set(empty);
      empty.deleteOnExit();
    }
    catch (IOException e) {
      LOG.info(e);
    }
  }

  @NotNull
  @Override
  protected JSLinterConfigurable<JscsState> createSettingsConfigurable(@NotNull Project project) {
    return new JscsConfigurable(project, true);
  }

  @Override
  protected Class<? extends JSLinterConfiguration<JscsState>> getConfigurationClass() {
    return JscsConfiguration.class;
  }

  @Override
  protected Class<? extends JSLinterInspection> getInspectionClass() {
    return JscsInspection.class;
  }

  @Override
  protected boolean acceptPsiFile(@NotNull PsiFile file) {
    return file instanceof JSFile && JSUtils.isJavaScriptFile(file);
  }

  @Nullable
  @Override
  public JSLinterAnnotationResult doAnnotate(@Nullable JSLinterInput<JscsState> collectedInfo) {
    if (collectedInfo == null) {
      return null;
    }
    return createJscsRunner(collectedInfo).execute();
  }

  @NotNull
  public JscsExternalRunner createJscsRunner(@NotNull JSLinterInput<JscsState> collectedInfo) {
    return new JscsExternalRunner(collectedInfo, myCodeFilesMirror, myConfigFilesMirror, collectedInfo.getProject(),
                                  () -> {
                                    tryCreateEmptyConfig();
                                    return myEmptyConfig.get();
                                  });
  }

  @Override
  public void apply(@NotNull final PsiFile file,
                    @Nullable final JSLinterAnnotationResult annotationResult,
                    @NotNull AnnotationHolder holder) {
    if (annotationResult == null) return;
    final boolean useDefault = annotationResult.getConfigFile() == null;
    final boolean noConfig = annotationResult.getConfigFile() != null &&
                             FileUtil.filesEqual(myEmptyConfig.get(), new File(annotationResult.getConfigFile().getPath()));


    new JSLinterAnnotationsBuilder(file, annotationResult, holder,
                                   new JscsConfigurable(file.getProject(), true), "JSCS: ", getInspectionClass(),
                                   new JSLinterStandardFixes().setEditConfig(false)
                                       .setErrorToIntentionConverter(errorBase -> {
                                         if (!useDefault) {
                                           if (!StringUtil.isEmptyOrSpaces(errorBase.getCode())) {
                                             final List<IntentionAction> list = new ArrayList<>(3);
                                             final VirtualFile configFile = annotationResult.getConfigFile();

                                             if (!noConfig) {
                                               final JSLinterEditConfigFileAction action = new JSLinterEditConfigFileAction(configFile);
                                               action.setProperty(errorBase.getCode());
                                               list.add(action);
                                             } else {
                                               list.add(new JscsCreateConfigFromPresetFix());
                                             }

                                             if (errorBase instanceof JSLinterError) {
                                               if (isOnTheFly()){
                                                 list.add(new JscsSuppressRuleFix(((JSLinterError)errorBase).getLine(), errorBase.getCode()));
                                               }
                                               list.add(new JscsSuppressRuleForFileFix(errorBase.getCode()));
                                               addExcludeFileInConfigFix(list, noConfig ? null : configFile, file);
                                             }

                                             list.add(new JscsFixAction().asIntentionAction());
                                             return list;
                                           }
                                           else if (errorBase instanceof JscsTypeError) {
                                             final VirtualFile configFile = annotationResult.getConfigFile();
                                             return Collections.singletonList(new EditFirstTypeErrorInConfig(configFile));
                                           }
                                         }
                                         return null;
                                       }))
      .setHighlightingGranularity(HighlightingGranularity.element).apply();
  }

  private static void addExcludeFileInConfigFix(List<IntentionAction> list, VirtualFile configFile, PsiFile file) {
    final String configPath = configFile == null ? file.getProject().getBasePath() : configFile.getParent().getPath();
    String relativePath = FileUtil.getRelativePath(FileUtil.toSystemIndependentName(configPath),
                                                   FileUtil.toSystemIndependentName(file.getVirtualFile().getPath()), '/');
    if (relativePath != null) {
      relativePath = correctRelativePathForJscs(relativePath);
      if (configFile == null) {
        list.add(new JscsCreateConfigAndExcludeFix(file.getName(), relativePath));
      } else {
        list.add(new JscsExcludeFileInConfigFix(configFile, file.getName(), relativePath));
      }
    }
  }

  private static String correctRelativePathForJscs(String relativePath) {
    if (relativePath == null) return null;
    if (! relativePath.startsWith(".")) {
      if (! relativePath.startsWith("/")) {
        relativePath = "/" + relativePath;
      }
      relativePath = "." + relativePath;
    }
    return relativePath;
  }

  public static class JscsSuppressRuleFix extends BaseIntentionAction {
    private final int myZeroBasedLineNumber;
    @NotNull
    private final String myCode;

    public JscsSuppressRuleFix(int zeroBasedLineNumber, @NotNull String code) {
      myZeroBasedLineNumber = zeroBasedLineNumber;
      myCode = code;
    }

    @NotNull
    @Override
    public String getFamilyName() {
      return "Suppress for current line";
    }

    @Override
    public boolean isAvailable(@NotNull Project project, Editor editor, PsiFile file) {
      return true;
    }

    @NotNull
    @Override
    public String getText() {
      return "Suppress " + myCode + " for current line";
    }

    @Override
    public void invoke(@NotNull Project project, Editor editor, PsiFile file) throws IncorrectOperationException {
      final Document document = JSLinterUtil.getDocumentForElement(file);
      if (document == null) return;
      final int offset = editor.getCaretModel().getOffset();
      JscsInspection.JscsSuppressForLineByCommentFix.suppressRuleForLine(project, document, file, myZeroBasedLineNumber, offset, myCode);
    }
  }

  public static class JscsSuppressRuleForFileFix extends BaseIntentionAction {
    @NotNull
    private final String myCode;

    public JscsSuppressRuleForFileFix(@NotNull String code) {
      myCode = code;
    }

    @NotNull
    @Override
    public String getFamilyName() {
      return "Suppress for current file";
    }

    @Override
    public boolean isAvailable(@NotNull Project project, Editor editor, PsiFile file) {
      return true;
    }

    @NotNull
    @Override
    public String getText() {
      return "Suppress " + myCode + " for file";
    }

    @Override
    public void invoke(@NotNull Project project, Editor editor, PsiFile file) throws IncorrectOperationException {
      final Document document = JSLinterUtil.getDocumentForElement(file);
      if (document == null) return;
      document.insertString(0, "// jscs:disable " + myCode + "\n");
    }
  }
}
