package com.intellij.lang.javascript.linter.jscs;

import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.process.CapturingProcessHandler;
import com.intellij.execution.process.ProcessOutput;
import com.intellij.javascript.nodejs.interpreter.NodeJsInterpreter;
import com.intellij.javascript.nodejs.interpreter.local.NodeJsLocalInterpreter;
import com.intellij.lang.javascript.linter.*;
import com.intellij.lang.javascript.linter.jscs.config.JscsConfigFileChangeTracker;
import com.intellij.lang.javascript.linter.jscs.config.JscsConfigFileSearcher;
import com.intellij.lang.javascript.linter.jscs.config.JscsConfigHelper;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.util.Getter;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.PathMatcher;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * @author by Irina.Chernushina on 9/23/2014.
 */
public class JscsExternalRunner {
  private static final Logger LOG = Logger.getInstance(JscsConfiguration.LOG_CATEGORY);
  @NonNls
  private static final String CONFIGURATION_FILE_FOR_JSCS_IS_NOT_FOUND = "Configuration file for JSCS is not found";
  public static final int TIMEOUT_IN_MILLISECONDS = (int)TimeUnit.SECONDS.toMillis(10);

  private final JSLinterInput<JscsState> myInputInfo;
  private final FilesMirror myCodeFilesMirror;
  private final FilesMirror myConfigFilesMirror;
  private final Project myProject;
  private final Getter<? extends File> myEmptyConfig;
  private boolean mySkip;
  private String mySkipReason;
  private final List<Supplier<JSLinterAnnotationResult>> mySteps;
  private File myActualConfigFile;
  private File myActualCodeFile;
  private File myNodeFile;
  private File myPackageDir;
  private VirtualFile myConfigVirtualFile;
  private VirtualFile myCodeVirtualFile;
  private File myJscsFile;
  private boolean myFix;
  private File myFoundConfigFile;

  public JscsExternalRunner(@NotNull final JSLinterInput<JscsState> info,
                            @NotNull final FilesMirror filesMirror,
                            @NotNull final FilesMirror configMirror,
                            Project project, Getter<? extends File> emptyConfigGetter) {
    myInputInfo = info;
    myCodeFilesMirror = filesMirror;
    myConfigFilesMirror = configMirror;
    myProject = project;
    myEmptyConfig = emptyConfigGetter;
    mySteps = new ArrayList<>();
    mySteps.add(checkExePath());
    mySteps.add(checkTargetVirtualFile());
    mySteps.add(checkConfigPath());
    mySteps.add(checkIfTargetFileChanged());
    mySteps.add(runExternalProcess());
  }

  public JSLinterAnnotationResult execute() {
    for (Supplier<JSLinterAnnotationResult> step : mySteps) {
      final JSLinterAnnotationResult result = step.get();
      if (result != null) return result;
      if (mySkip) return null;
    }
    return null;
  }

  public String getSkipReason() {
    return mySkipReason;
  }

  private void skip(final String reason) {
    mySkip = true;
    mySkipReason = reason;
  }

  private Supplier<JSLinterAnnotationResult> runExternalProcess() {
    return () -> {
      final File workingDirectory = myActualCodeFile.getParentFile();
      if (workingDirectory == null) {
        LOG.debug("Skipped JSCS file analysis: can not find working directory for file: " + myActualCodeFile.getPath());
        skip("Can not find working directory for file: " + myActualCodeFile.getPath());
        return null;
      }
      // we are ready to start jscs => have found some config file and should start tracking it
      JscsConfigFileChangeTracker.getInstance(myProject).startIfNeeded();

      String error = null;
      ProcessOutput processOutput = null;
      JscsCheckStyleOutputFormatParser parser = null;
      final GeneralCommandLine commandLine = createCommandLine(workingDirectory);
      CapturingProcessHandler processHandler;
      try {
        processHandler = new CapturingProcessHandler(commandLine);
      }
      catch (ExecutionException e) {
        error = "Can not start JSCS process: " + e.getMessage();
        processHandler = null;
      }
      if (processHandler != null) {
        parser = new JscsCheckStyleOutputFormatParser(myActualCodeFile.getPath());
        processHandler.addProcessListener(parser);
        processOutput = processHandler.runProcess(TIMEOUT_IN_MILLISECONDS);
        if (processOutput.isTimeout()) {
          return JSLinterAnnotationResult.create(myInputInfo, new JSLinterFileLevelAnnotation("Process timed out after " +
                                                                                              StringUtil.formatDurationApproximate(
                                                                                                (long)TIMEOUT_IN_MILLISECONDS)),
                                                 myConfigVirtualFile);
        }
        parser.process();

        error = parser.getGlobalError() == null ? null : parser.getGlobalError().getDescription();
      }

      if (error != null) {
        final IntentionAction details = processHandler == null ? null :
                                        JSLinterUtil.createDetailsAction(myProject, myCodeVirtualFile, commandLine, processOutput, null);
        return JSLinterAnnotationResult.create(myInputInfo, new JSLinterFileLevelAnnotation(error, details), myConfigVirtualFile);
      }
      final JscsObsoleteRulesWarning warning = parser.getObsoleteRulesWarning();
      if (warning != null) {
        return JSLinterAnnotationResult.createLinterResult(myInputInfo, new JSLinterFileLevelAnnotation(warning.getDescription()),
                                                           parser.getErrors(),
                                                           myConfigVirtualFile);
      }
      return JSLinterAnnotationResult.createLinterResult(myInputInfo, parser.getErrors(), myConfigVirtualFile);
    };
  }

  private JSLinterAnnotationResult createError(JSLinterFileLevelAnnotation error) {
    return JSLinterAnnotationResult.create(myInputInfo, error, myConfigVirtualFile);
  }

  private JSLinterAnnotationResult createError(String error) {
    return JSLinterAnnotationResult.create(myInputInfo, new JSLinterFileLevelAnnotation(error), myConfigVirtualFile);
  }

  @NotNull
  public GeneralCommandLine createCommandLine(@NotNull File workingDir) {
    GeneralCommandLine commandLine = new GeneralCommandLine();
    commandLine.withCharset(StandardCharsets.UTF_8);
    commandLine.withWorkDirectory(workingDir);
    commandLine.setExePath(myNodeFile.getAbsolutePath());

    commandLine.addParameter(myJscsFile.getAbsolutePath());

    if (myFix) {
      commandLine.addParameter("-x");
    } else {
      commandLine.addParameters("-v", "-r", "checkstyle");
    }
    final JscsPreset preset = myInputInfo.getState().getPreset();
    if (preset != null) {
      commandLine.addParameters("--preset", preset.getCode());
    }
    if (myActualConfigFile != null) {
      commandLine.addParameters("-c", myActualConfigFile.getAbsolutePath());
    }
    commandLine.addParameters(myActualCodeFile.getAbsolutePath());
    return commandLine;
  }

  public void setFix(boolean fix) {
    myFix = fix;
  }

  public Supplier<JSLinterAnnotationResult> checkExePath() {
    return () -> {
      final JscsState state = myInputInfo.getState();
      final NodeJsInterpreter interpreter = state.getInterpreterRef().resolve(myProject);
      final String nodePath = !(interpreter instanceof NodeJsLocalInterpreter) ? "" :
                              ((NodeJsLocalInterpreter)interpreter).getInterpreterSystemDependentPath();
      if (StringUtil.isEmptyOrSpaces(nodePath)) {
        return createError("Node interpreter file is not specified");
      }
      myNodeFile = new File(nodePath);
      if (! myNodeFile.isFile() || ! myNodeFile.isAbsolute() || ! myNodeFile.canExecute()) {
        return createError("Node interpreter file is not found");
      }
      final String packagePath = state.getPackagePath();
      if (StringUtil.isEmptyOrSpaces(packagePath)) {
        return createError(JSLinterUtil.createLinterPackageError(myProject, packagePath, "JSCS", null));
      }
      myPackageDir = new File(packagePath);
      if (myPackageDir.isDirectory()) {
        if (! myPackageDir.isAbsolute()) {
          return createError(JSLinterUtil.createLinterPackageError(myProject, packagePath, "JSCS", null));
        }
        myJscsFile = new File(myPackageDir, "bin" + File.separator + "jscs");
      } else {
        myJscsFile = myPackageDir;
      }
      if (! myJscsFile.exists()) {
        if (myPackageDir.exists() && myJscsFile == myPackageDir) {
          return createError(JSLinterUtil.createLinterPackageError(myProject, packagePath, "JSCS", null));
        } else {
          return createError("Can not find 'jscs' script file under JSCS package directory");
        }
      }
      return null;
    };
  }

  private Supplier<JSLinterAnnotationResult> checkConfigPath() {
    return () -> {
      final JscsState state = myInputInfo.getState();

      boolean configExactlyNamed = false;
      if (state.isCustomConfigFileUsed()) {
        final String configFilePath = state.getCustomConfigFilePath();
        if (StringUtil.isEmptyOrSpaces(configFilePath)) {
          return createError("Configuration file for JSCS is not specified");
        }
        myFoundConfigFile = new File(configFilePath);
      } else {
        final JscsConfigFileSearcher searcher = new JscsConfigFileSearcher(myProject, myCodeVirtualFile);
        searcher.lookup();
        if (searcher.getError() != null) {
          return createError(searcher.getError());
        }
        if (searcher.getPackage() != null) {
          myFoundConfigFile = searcher.getPackage();
          configExactlyNamed = true;
        } else if (searcher.getConfig() != null && searcher.getConfig().isFile()) {
          myFoundConfigFile = searcher.getConfig();
        } else {
          // if preset is used, then we can go without configuration file
          if (state.getPreset() != null) {
            myFoundConfigFile = myEmptyConfig.get();
            if (myFoundConfigFile == null || ! myFoundConfigFile.isFile() || ! myFoundConfigFile.isAbsolute()) {
              return createError("Can not create fictive empty config to run JSCS on (to ignore configs above project and use only preset) for file: " + myInputInfo.getPsiFile().getName());
            }
            myActualConfigFile = myFoundConfigFile;
            return null;
          } else {
            return createError(CONFIGURATION_FILE_FOR_JSCS_IS_NOT_FOUND);
          }
        }
      }

      if (! myFoundConfigFile.isFile() || ! myFoundConfigFile.isAbsolute()) {
        return createError(CONFIGURATION_FILE_FOR_JSCS_IS_NOT_FOUND);
      }
      myConfigVirtualFile = VfsUtil.findFileByIoFile(myFoundConfigFile, false);
      if (myConfigVirtualFile == null || ! myConfigVirtualFile.isValid()) {
        LOG.debug("Skipped JSCS file analysis: can not load config file as virtual file: " + myFoundConfigFile.getPath());
        skip("Can not load config file as virtual file: " + myFoundConfigFile.getPath());
        return null;
      }

      if (configIgnoresFile()) {
        LOG.debug("Skipped JSCS file analysis: ignored in config: " + myFoundConfigFile.getPath());
        skip("Ignored in config: " + myFoundConfigFile.getPath());
        return null;
      }
      myActualConfigFile = configExactlyNamed ? myConfigFilesMirror.getOrCreateExactlyNamed(myProject, myConfigVirtualFile, null) :
                           myConfigFilesMirror.getOrCreateFileWithActualContent(myProject, myConfigVirtualFile, null);
      if (myActualConfigFile == null) {
        LOG.debug("Skipped JSCS file analysis: can not mirror config file in temp directory: " + myFoundConfigFile.getPath());
        skip("Can not mirror config file in temp directory: " + myFoundConfigFile.getPath());
      }
      return null;
    };
  }

  private boolean configIgnoresFile() {
    final Application application = ApplicationManager.getApplication();

    final List<String> excludedPaths = application.runReadAction((Computable<List<String>>)() -> {
      final Document document = FileDocumentManager.getInstance().getDocument(myConfigVirtualFile);
      if (document == null) return Collections.emptyList();
      return JscsConfigHelper.getExcludedPaths(myProject, document);
    });

    if (excludedPaths == null || excludedPaths.isEmpty()) return false;

    //glob matching; either Java 7 or Apache FilenameUtils from commons can be used.
    final File codeFile = new File(FileUtil.toSystemDependentName(myCodeVirtualFile.getPath()));
    String relativeCodeFile = FileUtil.getRelativePath(new File(myConfigVirtualFile.getParent().getPath()), codeFile);
    if (relativeCodeFile == null) return false;
    final String relativeStartingWithDot = ensureStartWithDot(FileUtil.toSystemIndependentName(relativeCodeFile));
    FileSystem fs = FileSystems.getDefault();
    for (String path : excludedPaths) {
      path = ensureStartWithDot(path);
      PathMatcher matcher = fs.getPathMatcher("glob:" + path);
      if (matcher.matches(fs.getPath(relativeStartingWithDot)) ||
          matcher.matches(fs.getPath(FileUtil.toSystemIndependentName(relativeCodeFile)))) return true;
    }
    return false;
  }

  private static String ensureStartWithDot(final String path) {
    if (FileUtil.isAbsolute(path)) return path;
    if (path.startsWith("*") || path.endsWith("*")) return path;
    if (path.startsWith(".")) return path;  // either ../ or ./
    if (path.startsWith("/")) return "." + path;
    return "./" + path;
  }

  private Supplier<JSLinterAnnotationResult> checkTargetVirtualFile() {
    return () -> {
      final PsiFile psiFile = myInputInfo.getPsiFile();
      myCodeVirtualFile = psiFile.getVirtualFile();
      if (myCodeVirtualFile == null || !myCodeVirtualFile.isValid()) {
        LOG.debug("Skipped JSCS file analysis: can not load target file as virtual file: " + psiFile.getName());
        skip("Can not load target file as virtual file: " + psiFile.getName());
        return null;
      }
      return null;
    };
  }

  private Supplier<JSLinterAnnotationResult> checkIfTargetFileChanged() {
    return () -> {
      final PsiFile psiFile = myInputInfo.getPsiFile();
      myActualCodeFile = myCodeFilesMirror.getOrCreateFileWithActualContent(myProject, myCodeVirtualFile, myInputInfo.getFileContent());
      if (myActualCodeFile == null) {
        LOG.debug("Skipped JSCS file analysis: can not mirror target file in temp directory: " + psiFile.getName());
        skip("Can not mirror target file in temp directory: " + psiFile.getName());
      }
      return null;
    };
  }
}
