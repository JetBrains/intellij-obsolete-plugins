package org.jetbrains.plugins.ruby.chef.lint.foodcritic;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.ExecutionModes;
import com.intellij.execution.Platform;
import com.intellij.execution.process.ProcessOutput;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.util.SimpleModificationTracker;
import com.intellij.openapi.util.registry.Registry;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiTreeChangeAdapter;
import com.intellij.psi.PsiTreeChangeEvent;
import com.intellij.psi.util.CachedValueProvider;
import com.intellij.psi.util.CachedValuesManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.ruby.chef.ChefUtil;
import org.jetbrains.plugins.ruby.gem.RubyGemExecutionContext;
import org.jetbrains.plugins.ruby.ruby.RModuleUtil;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class FoodcriticCache implements Disposable {
  public static final String FOODCRITIC_GEM_NAME = "foodcritic";
  static final Logger LOG = Logger.getInstance(FoodcriticCache.class);
  private static final Pattern FOODCRITIC_WARNING_PATTERN = Pattern.compile("(FC\\d{3}): (.+): (.+):(\\d+)");
  ConcurrentMap<String, SimpleModificationTracker> myModificationTrackerMap = new ConcurrentHashMap<>();

  FoodcriticCache(@NotNull Project project) {
    PsiManager.getInstance(project).addPsiTreeChangeListener(new PsiTreeChangeAdapter() {
      @Override
      public void childrenChanged(@NotNull PsiTreeChangeEvent event) {
        final PsiFile file = event.getFile();
        if (file == null) return;

        final PsiDirectory cookbook = ChefUtil.getCookbookByFileInside(file);
        if (cookbook == null) return;

        final SimpleModificationTracker modificationTracker = myModificationTrackerMap.get(cookbook.getVirtualFile().getPath());

        if (modificationTracker != null) {
          modificationTracker.incModificationCount();
        }
      }
    }, this);
  }

  @Override
  public void dispose() {
  }

  public @NotNull FoodcriticCookbookCache getOrCreateCookbookCache(final @NotNull PsiDirectory cookbook) {
    return CachedValuesManager.getCachedValue(cookbook, () -> {
      final FoodcriticCookbookCache cookbookCache = new FoodcriticCookbookCache();
      final String path = cookbook.getVirtualFile().getPath();

      SimpleModificationTracker modificationTracker = new SimpleModificationTracker();
      final SimpleModificationTracker previousValue = myModificationTrackerMap.putIfAbsent(path, modificationTracker);
      if (previousValue != null) {
        modificationTracker = previousValue;
      }

      return CachedValueProvider.Result.create(cookbookCache, modificationTracker);
    });
  }

  public static @NotNull FoodcriticCache getInstance(final @NotNull Project project) {
    return project.getService(FoodcriticCache.class);
  }

  static void collectWarnings(final @NotNull FoodcriticCookbookCache cookbookCache, PsiDirectory cookbook) {
    final Module module = ModuleUtilCore.findModuleForPsiElement(cookbook);
    if (module == null) return;

    final Sdk sdk = RModuleUtil.getInstance().findRubySdkForModule(module);
    if (sdk == null) return;

    final FileDocumentManager fileDocumentManager = FileDocumentManager.getInstance();
    if (!ApplicationManager.getApplication().isReadAccessAllowed()) {
      ApplicationManager.getApplication().invokeAndWait(() -> fileDocumentManager.saveAllDocuments());
    }

    final VirtualFile originalCookbook = cookbook.getVirtualFile();
    final String cookbookPath = originalCookbook.getPath();

    try {
      ProcessOutput output = RubyGemExecutionContext.create(sdk, FOODCRITIC_GEM_NAME)
        .withModule(module)
        .withWorkingDir(cookbook.getProject().getBaseDir())
        .withExecutionMode(new ExecutionModes.SameThreadMode(false, null, Registry.intValue("ruby.chef.foodcritic.timeout", 150)))
        .withArguments(cookbookPath)
        .executeScript();

      if (output == null) return;
      if (output.isTimeout()) {
        LOG.info("Timeout running foodcritic");
        return;
      }

      cookbookCache.init();

      final List<String> stdoutLines = output.getStdoutLines();
      if (stdoutLines.isEmpty()) {
        return;
      }

      for (String line : stdoutLines) {
        final FoodcriticProblem problem = parseProblem(line, cookbookPath);
        if (problem != null) {
          cookbookCache.cacheValue(problem);
        }
      }
    }
    catch (ExecutionException e) {
      LOG.warn("Foodcritic thread execution failed: " + e.getMessage());
    }
  }

  private static @Nullable FoodcriticProblem parseProblem(final @NotNull String problemText, final @NotNull String cookbookPath) {
    Matcher m = FOODCRITIC_WARNING_PATTERN.matcher(problemText);
    if (m.matches()) {
      final String id = m.group(1);
      final String description = m.group(2);
      String path = m.group(3);

      final String cookbookWithSeparator = cookbookPath + Platform.current().fileSeparator;
      if (path.startsWith(cookbookWithSeparator)) path = StringUtil.substringAfter(path, cookbookWithSeparator);

      int lineNumber = 1;
      try {
        lineNumber = Integer.parseInt(m.group(4));
      }
      catch (NumberFormatException e) {
        LOG.error("Foodcritic warning output - cannot parse line number. Text: " + problemText);
      }

      if (path == null) return null;
      return new FoodcriticProblem(path, id + ": " + description, lineNumber);
    }
    return null;
  }
}
