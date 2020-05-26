package com.intellij.lang.javascript.linter.gjslint;

import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.process.*;
import com.intellij.javascript.nodejs.NodeCommandLineUtil;
import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.lang.javascript.linter.*;
import com.intellij.lang.javascript.psi.JSFile;
import com.intellij.lang.javascript.psi.util.JSUtils;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GjsLintExternalAnnotator extends JSLinterExternalAnnotator<GjsLintState> {

  private static final Logger LOG = Logger.getInstance(GjsLintExternalAnnotator.class);
  private static final GjsLintExternalAnnotator INSTANCE_FOR_BATCH_INSPECTION = new GjsLintExternalAnnotator(false);
  private static final Pattern ERROR_PATTERN = Pattern.compile("^Line (\\d+), E:\\d+: (.*)$");
  private static final String CODE_TEMP_FILE_MAP_KEY_NAME = "CODE_TEMP_FILE_MAP_KEY";
  private static final String CONFIG_TEMP_FILE_MAP_KEY_NAME = "CONFIG_TEMP_FILE_MAP_KEY";

  private final FilesMirror myCodeFilesMirror;
  private final FilesMirror myConfigFilesMirror;

  @SuppressWarnings("unused")
  public GjsLintExternalAnnotator() {
    this(true);
  }

  public GjsLintExternalAnnotator(boolean onTheFly) {
    super(onTheFly);
    myCodeFilesMirror = new FilesMirror(CODE_TEMP_FILE_MAP_KEY_NAME, "intellij-js-closure-linter");
    myConfigFilesMirror = new FilesMirror(CONFIG_TEMP_FILE_MAP_KEY_NAME, "intellij-js-closure-linter");
  }

  @NotNull
  public static GjsLintExternalAnnotator getInstanceForBatchInspection() {
    return INSTANCE_FOR_BATCH_INSPECTION;
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
    return file instanceof JSFile && JSUtils.isJavaScriptFile(file);
  }

  @Nullable
  @Override
  public JSLinterAnnotationResult annotate(@NotNull JSLinterInput<GjsLintState> collectedInfo) {
    GjsLintState state = collectedInfo.getState();
    String exeFilePath = state.getLinterExePath();
    if (StringUtil.isEmpty(exeFilePath)) {
      return JSLinterAnnotationResult.create(collectedInfo, new JSLinterFileLevelAnnotation("Closure Linter executable file is not specified"), null);
    }
    File exeFile = new File(exeFilePath);
    if (!exeFile.isFile() || !exeFile.isAbsolute() || !exeFile.canExecute()) {
      return JSLinterAnnotationResult.create(collectedInfo, new JSLinterFileLevelAnnotation("Closure Linter executable file is not found"), null);
    }

    String configFilePath = state.getConfigFilePath();
    if (StringUtil.isEmpty(configFilePath)) {
      return JSLinterAnnotationResult.create(collectedInfo, new JSLinterFileLevelAnnotation("Configuration file for Closure Linter is not specified"), null);
    }
    File configFile = new File(configFilePath);
    if (!configFile.isFile() || !configFile.isAbsolute()) {
      return JSLinterAnnotationResult.create(collectedInfo, new JSLinterFileLevelAnnotation("Configuration file for Closure Linter is not found"), null);
    }

    VirtualFile codeVirtualFile = collectedInfo.getPsiFile().getVirtualFile();
    if (codeVirtualFile == null || !codeVirtualFile.isValid()) {
      return null;
    }
    VirtualFile configVirtualFile = VfsUtil.findFileByIoFile(configFile, false);
    if (configVirtualFile == null || !configVirtualFile.isValid()) {
      return null;
    }
    File actualCodeFile = myCodeFilesMirror.getOrCreateFileWithActualContent(collectedInfo.getProject(), codeVirtualFile, collectedInfo.getFileContent());
    if (actualCodeFile == null) {
      return null;
    }
    File actualConfigFile = myConfigFilesMirror.getOrCreateFileWithActualContent(collectedInfo.getProject(), configVirtualFile, null);
    if (actualConfigFile == null) {
      return null;
    }
    GjsLintConfigFileChangeTracker.getInstance(collectedInfo.getProject());
    long startTimeNano = System.nanoTime();
    try {
      return startProcess(collectedInfo, exeFile, configVirtualFile, actualConfigFile, codeVirtualFile, actualCodeFile);
    } finally {
      long durationNano = System.nanoTime() - startTimeNano;
      if (durationNano > TimeUnit.MILLISECONDS.toNanos(500)) {
        LOG.info("[Closure Linter] Taken time " + String.format("%d ms", TimeUnit.NANOSECONDS.toMillis(durationNano)));
      }
    }
  }

  @Nullable
  private static JSLinterAnnotationResult startProcess(@NotNull JSLinterInput<GjsLintState> collectedInfo,
                                                       @NotNull File exeFile,
                                                       @NotNull VirtualFile configVirtualFile,
                                                       @NotNull File configFile,
                                                       @NotNull VirtualFile codeVirtualFile,
                                                       @NotNull File codeFile) {
    File workingDir = codeFile.getParentFile();
    if (workingDir == null) {
      return null;
    }
    GeneralCommandLine commandLine = createCommandLine(workingDir, exeFile,
                                                       configVirtualFile, configFile,
                                                       codeVirtualFile, codeFile);
    String runError = null;
    OSProcessHandler processHandler;
    final ProcessOutput output = new ProcessOutput();
    try {
      processHandler = NodeCommandLineUtil.createKillableColoredProcessHandler(commandLine, true);
    }
    catch (ExecutionException e) {
      runError = "Can not start gjslint process: " + e.getMessage();
      processHandler = null;
    }
    final List<JSLinterError> errors = new ArrayList<>();
    final Ref<String> runErrorRef = Ref.create(runError);

    if (runErrorRef.isNull() && processHandler != null) {
      processHandler.addProcessListener(new ProcessAdapter() {
        @Override
        public void onTextAvailable(@NotNull ProcessEvent event, @NotNull Key outputType) {
          if (event == null) {
            return;
          }
          String text = event.getText().trim();
          if (outputType == ProcessOutputTypes.STDOUT) {
            output.appendStdout(text);
            JSLinterError error = toError(text);
            if (error != null) {
              errors.add(error);
            }
          }
          else if (outputType == ProcessOutputTypes.STDERR) {
            output.appendStderr(text);
            String prefix = "gflags.UnrecognizedFlagError: ";
            if (text.startsWith(prefix) && runErrorRef.isNull()) {
              String unrecognizedFlag = text.substring(prefix.length());
              String fix = findPossibleFixedFlag(unrecognizedFlag);
              StringBuilder message = new StringBuilder("Closure Linter: ").append(unrecognizedFlag);
              if (fix != null) {
                message.append(". Try '").append(fix).append("'");
              }
              runErrorRef.set(message.toString());
            }
          }
        }
      });
      processHandler.startNotify();
      processHandler.waitFor();
    }
    runError = runErrorRef.get();
    if (runError != null) {
      final IntentionAction detailsAction = processHandler == null ? null :
                                     JSLinterUtil.createDetailsAction(collectedInfo.getProject(), configVirtualFile, commandLine, output, null);
      return JSLinterAnnotationResult.create(collectedInfo, new JSLinterFileLevelAnnotation(null, runError, HighlightSeverity.ERROR,
                   new JSLinterStandardFixes().setDetailsAction(detailsAction)), configVirtualFile);
    }
    return JSLinterAnnotationResult.createLinterResult(collectedInfo, errors, configVirtualFile);
  }

  @Nullable
  private static String findPossibleFixedFlag(@NotNull String message) {
    String prefix = "Unknown command line flag '";
    String suffix = "'";
    if (message.startsWith(prefix) && message.endsWith(suffix)) {
      String unrecognizedFlag = message.substring(prefix.length(), message.length() - suffix.length());
      unrecognizedFlag = unrecognizedFlag.trim();
      int ind = unrecognizedFlag.indexOf(" ");
      if (ind > 0) {
        String first = unrecognizedFlag.substring(0, ind).trim();
        String second = unrecognizedFlag.substring(ind + 1).trim();
        return first + "=" + second;
      }
    }
    return null;
  }

  @Nullable
  private static JSLinterError toError(@NotNull String text) {
    Matcher matcher = ERROR_PATTERN.matcher(text);
    if (matcher.find() && matcher.groupCount() == 2) {
      String lineStr = matcher.group(1);
      final int line;
      try {
        line = Integer.parseInt(lineStr);
      } catch (NumberFormatException ignored) {
        LOG.warn("Can't parse line number in '" + lineStr + "'");
        return null;
      }
      String message = matcher.group(2);
      return new JSLinterError(line, 0, message, null);
    }
    return null;
  }

  @NotNull
  public static GeneralCommandLine createCommandLine(@NotNull File workingDir,
                                                     @NotNull File gjslintExeFile,
                                                     @NotNull VirtualFile configVirtualFile,
                                                     @NotNull File configFile,
                                                     @NotNull VirtualFile codeVirtualFile,
                                                     @NotNull File codeFile) {
    GeneralCommandLine commandLine = new GeneralCommandLine();
    commandLine.withParentEnvironmentType(GeneralCommandLine.ParentEnvironmentType.CONSOLE);
    commandLine.setCharset(StandardCharsets.UTF_8);
    commandLine.setWorkDirectory(workingDir);
    commandLine.setExePath(gjslintExeFile.getAbsolutePath());
    commandLine.addParameter("--flagfile");
    commandLine.addParameters(configFile.getAbsolutePath());
    commandLine.addParameter("--recurse=no");
    commandLine.addParameter(codeFile.getAbsolutePath());
    boolean passRealFilePath = GjsLintConfigFileChangeTracker.checkPassRealPath(configVirtualFile);
    if (passRealFilePath) {
      String configRealPath = FileUtil.toSystemDependentName(codeVirtualFile.getPath());
      commandLine.addParameter("--realFilePath=" + configRealPath);
    }
    return commandLine;
  }

  @Override
  public void apply(@NotNull PsiFile file,
                    @Nullable JSLinterAnnotationResult annotationResult,
                    @NotNull AnnotationHolder holder) {
    if (annotationResult == null) return;

    Document document = PsiDocumentManager.getInstance(file.getProject()).getDocument(file);
    if (document == null) {
      return;
    }
    GjsLintConfigurable configurable = new GjsLintConfigurable(file.getProject(), true);

    new JSLinterAnnotationsBuilder(file, annotationResult, holder, configurable, "Closure Linter: ",
                                   getInspectionClass(), JSLinterStandardFixes.DEFAULT)
      .setHighlightingGranularity(HighlightingGranularity.line).apply();
  }
}
