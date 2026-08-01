package org.jetbrains.jps.gwt.build;

import com.intellij.execution.process.BaseOSProcessHandler;
import com.intellij.execution.process.ProcessOutputType;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.NlsSafe;
import com.intellij.openapi.util.SystemInfoRt;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.containers.FactoryMap;
import com.intellij.util.io.URLUtil;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jps.gwt.GwtJpsBundle;
import org.jetbrains.jps.gwt.build.common.OutputLineReader;
import org.jetbrains.jps.incremental.CompileContext;
import org.jetbrains.jps.incremental.messages.BuildMessage;
import org.jetbrains.jps.incremental.messages.CompilerMessage;
import org.jetbrains.jps.incremental.messages.ProgressMessage;
import org.jetbrains.jps.service.SharedThreadPool;
import org.jetbrains.jps.util.JpsPathUtil;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Future;

public final class GwtExternalCompilerProcessHandler extends BaseOSProcessHandler {
  private static final Logger LOG = Logger.getInstance(GwtExternalCompilerProcessHandler.class);
  private static final @NonNls String[] COMPILATION_PROGRESS_PREFIXES = {
    "Analyzing source",
    "Copying all files found on public path",
    "Output will be written into",
    "Loading inherited module",
    "Refreshing resources",
    "Compiling module",
    "Compiling permutation"
  };
  private static final @NonNls String FINDING_ENTRY_POINTS_PREFIX = "Finding entry point classes";
  private static final @NonNls String ERROR_PREFIX = "[ERROR] ";
  private static final @NonNls String WARNING_PREFIX = "[WARN] ";
  private static final @NonNls String[] IGNORED_PATTERNS_IN_STDERR = {"INFO:", "com.vaadin.terminal.gwt.widgetsetutils.ClassPathExplorer",
    "Picked up _JAVA_OPTIONS:", "Picked up JAVA_TOOL_OPTIONS:"};
  private static final @NonNls String ERROR_FILE_PREFIX = "Errors in ";
  private static final @NonNls String ERROR_LINE_PREFIX = "Line ";
  private static final @NonNls String ERROR_LINE_SUFFIX = ": ";
  private static final @NonNls String BUILD_FAILED_MESSAGE = "Build failed";
  private static final @NonNls String STACKTRACE_PREFIX = "at ";
  private static final @NonNls Set<String>
    MODULE_FILE_ERRORS = new HashSet<>(Collections.singletonList("Module has no entry points defined"));
  private static final @NonNls String[] CLASS_NAME_PREFIXES = {"Type ", "Return type: ", "Parameter: "};
  private final Collection<? extends File> mySourceRoots;
  private final CompileContext myContext;
  private final Map<Key<?>, GwtCompilerOutputParser> myParsers =
    FactoryMap.create(key -> new GwtCompilerOutputParser(ProcessOutputType.isStderr(key)));
  private final String myModuleFileUrl;

  public GwtExternalCompilerProcessHandler(Process process,
                                           @NotNull String commandLine,
                                           String moduleFileUrl,
                                           Collection<? extends File> sourceRoots,
                                           CompileContext context) {
    super(process, commandLine, null);
    myModuleFileUrl = moduleFileUrl;
    mySourceRoots = sourceRoots;
    myContext = context;
  }

  @Override
  public @NotNull Future<?> executeTask(@NotNull Runnable task) {
    return SharedThreadPool.getInstance().submit(task);
  }

  private String findFileUrlByClassName(String className) {
    for (File root : mySourceRoots) {
      List<String> names = StringUtil.split(className, ".");
      File currentDir = root;
      for (String name : names) {
        File file = new File(currentDir, name + ".java");
        if (file.isFile()) {
          return file.getAbsolutePath();
        }
        File next = new File(currentDir, name);
        if (next.isDirectory()) {
          currentDir = next;
        }
        else {
          break;
        }
      }
    }
    return null;
  }

  private boolean isCanceled() {
    return myContext.isCanceled();
  }

  private void reportMessage(@Nls String message, boolean isError, @Nullable String fileUrl, int lineNumber, int columnNum) {
    myContext.processMessage(new CompilerMessage(GwtBuilder.getCompilerName(), isError ? BuildMessage.Kind.ERROR : BuildMessage.Kind.WARNING, message,
                                                 fileUrl != null ? JpsPathUtil.urlToPath(fileUrl) : null, -1, -1, -1, lineNumber, columnNum));
  }

  private void showProgressMessage(@Nls String message) {
    myContext.processMessage(new ProgressMessage(message));
  }

  private String getAbsolutePath(String path) {
    if (new File(path).exists()) {
      return path;
    }
    for (File root : mySourceRoots) {
      File file = new File(root, path);
      if (file.exists()) {
        return file.getAbsolutePath();
      }
    }
    return path;
  }

  @Override
  public void notifyTextAvailable(final @NotNull String text, final @NotNull Key outputType) {
    super.notifyTextAvailable(text, outputType);
    myParsers.get(outputType).parseOutput(text);

    destroyProcessIfCanceled();
  }

  private void destroyProcessIfCanceled() {
    final boolean canceled = isCanceled();
    if (canceled) {
      destroyProcess();
    }
  }

  private String fixFileUrl(String url) {
    url = URLUtil.unescapePercentSequences(url);
    if (url.contains(":/")) {
      int idx = url.indexOf(":/");
      if (idx >= 0 && idx + 2 < url.length() && url.charAt(idx + 2) != '/') {
        String prefix = url.substring(0, idx);
        String suffix = url.substring(idx + 2);

        if (SystemInfoRt.isWindows) {
          url = prefix + "://" + suffix;
        }
        else {
          url = prefix + ":///" + suffix;
        }
      }
      return url;
    }
    return JpsPathUtil.pathToUrl(FileUtil.toSystemIndependentName(getAbsolutePath(url)));
  }

  public void waitForTerminationOrCancellation() {
    while (!waitFor(300)) {
      destroyProcessIfCanceled();
    }
  }

  private class GwtCompilerOutputParser extends OutputLineReader {
    private String myCurrentFileUrl;
    private int myCurrentClassIsActualCountdown;
    private boolean myFindingEntryPoints = false;
    private boolean myStackTraceExpected = false;
    private final boolean myErrorStream;

    GwtCompilerOutputParser(final boolean isErrorStream) {
      myErrorStream = isErrorStream;
    }

    @Override
    protected void parseLine(@NotNull String line) {
      if (line.isEmpty()) return;

      if (LOG.isDebugEnabled()) {
        LOG.debug((myErrorStream ? "[stderr]" : "") + line);
      }

      for (String prefix : COMPILATION_PROGRESS_PREFIXES) {
        if (line.startsWith(prefix)) {
          showProgressMessage(line);
        }
      }

      if (line.startsWith(ERROR_FILE_PREFIX)) {
        myStackTraceExpected = false;
        setCurrentFileUrl(line.substring(ERROR_FILE_PREFIX.length()));
      }
      else if (line.startsWith(WARNING_PREFIX)) {
        String message = line.substring(WARNING_PREFIX.length());
        reportMessage(message, false, myCurrentFileUrl, -1, -1);
      }
      else if (line.startsWith(ERROR_PREFIX)) {
        myStackTraceExpected = false;
        boolean errorLineParsed = false;
        int start = ERROR_PREFIX.length();
        if (line.startsWith(ERROR_FILE_PREFIX, start)) {
          start += ERROR_FILE_PREFIX.length();
          int first = line.indexOf('\'', start);
          int last = line.lastIndexOf('\'');
          if (first != -1 && last != -1) {
            setCurrentFileUrl(line.substring(first + 1, last));
            errorLineParsed = true;
          }
        }
        else if (line.startsWith(ERROR_LINE_PREFIX, start)) {
          start += ERROR_LINE_PREFIX.length();
          final int end = line.indexOf(ERROR_LINE_SUFFIX, start);
          if (end != -1) {
            try {
              int lineNumber = Integer.parseInt(line.substring(start, end));
              String message = line.substring(end + ERROR_LINE_SUFFIX.length());
              reportMessage(message, true, myCurrentFileUrl, lineNumber, 0);
              errorLineParsed = true;
            }
            catch (NumberFormatException ignored) {
            }
          }
        }
        else {
          line = line.substring(start);
        }

        if (MODULE_FILE_ERRORS.contains(line) || myFindingEntryPoints) {
          reportMessage(line, true, myModuleFileUrl, -1, -1);
          errorLineParsed = true;
        }

        if (!errorLineParsed && !BUILD_FAILED_MESSAGE.equals(line)) {
          reportMessage(line, true, null, -1, -1);
          myStackTraceExpected = true;
        }
      }
      else if (line.startsWith(FINDING_ENTRY_POINTS_PREFIX)) {
        myFindingEntryPoints = true;
      }
      else if (line.startsWith(STACKTRACE_PREFIX) && myStackTraceExpected) {
        reportMessage(line, true, null, -1, -1);
      }
      else if (myErrorStream) {
        processStderrLine(line);
      }
      else {
        for (String prefix : CLASS_NAME_PREFIXES) {
          if (line.startsWith(prefix)) {
            int start = prefix.length();
            int end = line.indexOf(' ', start);
            if (end == -1) end = line.length();
            String url = findFileUrlByClassName(line.substring(start, end));
            if (url != null) {
              myCurrentFileUrl = url;
              myCurrentClassIsActualCountdown = 5;
            }
            break;
          }
        }
        myFindingEntryPoints = false;
      }
      if (myCurrentFileUrl != null && myCurrentClassIsActualCountdown > 0) {
        myCurrentClassIsActualCountdown--;
        if (myCurrentClassIsActualCountdown == 0) {
          myCurrentFileUrl = null;
        }
      }
    }

    private void processStderrLine(@NlsSafe String line) {
      for (String s : IGNORED_PATTERNS_IN_STDERR) {
        if (line.contains(s)) {
          return;
        }
      }
      reportMessage(GwtJpsBundle.message("compiler.message.stderr.0", line), false, null, -1, -1);
    }

    private void setCurrentFileUrl(final String url) {
      myCurrentFileUrl = fixFileUrl(url);
    }
  }
}
