package com.jetbrains.plugins.compass;

import com.google.common.base.Splitter;
import com.intellij.execution.ExecutionException;
import com.intellij.execution.process.ProcessOutput;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.roots.LibraryOrderEntry;
import com.intellij.openapi.roots.ModifiableModelsProvider;
import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.openapi.roots.OrderRootType;
import com.intellij.openapi.roots.impl.OrderEntryUtil;
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.openapi.roots.libraries.LibraryTable;
import com.intellij.openapi.roots.libraries.LibraryTablesRegistrar;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.psi.search.FilenameIndex;
import com.intellij.util.Processor;
import com.intellij.util.containers.ContainerUtil;
import com.jetbrains.plugins.compass.ruby.RubyCompassExtension;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.ruby.gem.GemInfo;
import org.jetbrains.plugins.ruby.gem.GemManager;
import org.jetbrains.plugins.sass.extensions.SassExtension;
import org.jetbrains.plugins.sass.extensions.SassRubyIntegrationHelper;
import com.jetbrains.plugins.compass.ruby.CompassConfigurableForRubyModule;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import static com.intellij.openapi.vfs.VfsUtilCore.pathToUrl;
import static com.intellij.util.containers.ContainerUtil.map;
import static com.intellij.util.containers.ContainerUtil.skipNulls;

public class CompassUtil {
  public static final String COMPASS_LIBRARY_NAME = "compass-library";
  public static final String COMPASS_EXECUTABLE_BASE_NAME = "compass";
  public static final String COMPASS_GEM_NAME = "compass";
  public static final String COMPASS_EXECUTABLE_BASE_NAME_WIN = "compass.bat";
  public static final String COMPASS_EXECUTABLE_RELATIVE_PATH = "bin/compass";
  public static final String COMPASS_EXECUTABLE_RELATIVE_PATH_WIN = "bin/compass.bat";

  public static final String COMPASS_STYLESHEETS_RELATIVE_PATH = "../../frameworks/compass/stylesheets/";
  public static final String CONFIG_RB = "config.rb";

  private static final Splitter COMPASS_IMPORTS_RESULT_SPLITTER = Splitter.on(' ').trimResults();
  private static final String COMPASS_IMPORTS_NOTIFICATION_ID = "Compass Execution Failures";
  private static final String GEMS_PATH_PART = "/gems/";
  private static final String COMPASS_LOGGING_CATEGORY = "compass";

  @NotNull
  public static List<String> getExecutableFilesVariants() {
    return SystemInfo.isWindows
           ? GemUtil.listPossibleExecutableFilePaths(COMPASS_EXECUTABLE_BASE_NAME_WIN, COMPASS_EXECUTABLE_RELATIVE_PATH_WIN)
           : GemUtil.listPossibleExecutableFilePaths(COMPASS_EXECUTABLE_BASE_NAME, COMPASS_EXECUTABLE_RELATIVE_PATH);
  }

  @Nullable
  public static CompassSassExtension getCompassExtension() {
    return SassRubyIntegrationHelper.getInstance().hasRubyPlugin()
            ? SassExtension.EXTENSION_POINT_NAME.findExtension(RubyCompassExtension.class)
            : SassExtension.EXTENSION_POINT_NAME.findExtension(CompassSassExtension.class);
  }

  @NotNull
  public static List<String> getConfigFileVariants(@NotNull Module module) {
    return skipNulls(map(
      FilenameIndex.getFilesByName(module.getProject(), CONFIG_RB, module.getModuleContentScope()),
      file -> {
        final VirtualFile virtualFile = file.getVirtualFile();
        return virtualFile != null ? virtualFile.getPath() : null;
      }));
  }

  /**
   * Executes 'compass imports' parameters, extracts and process all import paths that passes to sass.
   * Parent directory of given config uses as working directory of execution.
   */
  public static boolean runCompassImportsAndProcessPaths(@NotNull Module module,
                                                         @Nullable VirtualFile configFile,
                                                         @NotNull Processor<? super String> pathProcessor,
                                                         boolean showNotificationOnError) {
    if (ApplicationManager.getApplication().isUnitTestMode()) {
      final String compassFileUrl = pathToUrl(CompassSettings.getInstance(module).getCompassExecutableFilePath());
      final VirtualFile compassFile = VirtualFileManager.getInstance().refreshAndFindFileByUrl(compassFileUrl);
      if (compassFile != null) {
        final VirtualFile stylesheet = compassFile.findFileByRelativePath(COMPASS_STYLESHEETS_RELATIVE_PATH);
        if (stylesheet != null) {
          pathProcessor.process(stylesheet.getPath());
          return true;
        }
      }
      return false;
    }

    ProcessOutput output;
    String workingDirectoryPath = null;
    if (configFile != null) {
      final VirtualFile workingDirectory = configFile.getParent();
      if (workingDirectory != null && workingDirectory.isDirectory()) {
        workingDirectoryPath = workingDirectory.getPath();
      }
    }

    try {
      final CompassSettings settings = CompassSettings.getInstance(module);
      String executablePath = settings.getCompassExecutableFilePath();
      if (!executablePath.trim().isEmpty()) {
        output = SassRubyIntegrationHelper.getInstance().execScript(module, workingDirectoryPath, executablePath, "imports");
      }
      else {
        return false;
      }
    }
    catch (ExecutionException e) {
      output = new ProcessOutput(1);
      output.appendStderr(e.getMessage());
    }

    if (output.getExitCode() == 0) {
      processCompassImportsOutput(output.getStdout(), pathProcessor);
      return true;
    }
    else if (showNotificationOnError) {
      final String notificationMessage = configFile != null
              ? "Failed to run compass on " + configFile.getPath()
              : "Failed to run compass";
      Notifications.Bus.notify(new Notification(COMPASS_IMPORTS_NOTIFICATION_ID, "Compass", notificationMessage, NotificationType.WARNING));
    }
    Logger.getInstance(COMPASS_LOGGING_CATEGORY).info("Failed to run compass imports\n" + "Result code: " + output +
                                                      "\nout:\n" + output.getStdout()
                                                      + "\nerr:\n" + output.getStderr());
    return false;
  }

  public static void processCompassImportsOutput(@NotNull String output, @NotNull Processor<? super String> pathProcessor) {
    final Iterator<String> wordsIterator = COMPASS_IMPORTS_RESULT_SPLITTER.split(output).iterator();
    if (!wordsIterator.hasNext()) {
      Logger.getInstance(COMPASS_LOGGING_CATEGORY).info("Failed to run compass imports\n" + output);
    }
    while (wordsIterator.hasNext()) {
      if ("-I".equals(wordsIterator.next()) && wordsIterator.hasNext()) {
        final String newPath = wordsIterator.next();
        if (!newPath.isEmpty()) {
          pathProcessor.process(newPath);
        }
      }
    }
  }

  /**
   * Update module libraries according to compass settings:
   * - add or update library {@link this#COMPASS_LIBRARY_NAME}
   * - add import_paths extracted from output of 'compass imports' execution
   * - add import_paths extracted from config file to library
   *
   * @param compassSettings
   */
  public static void updateCompassLibraries(@NotNull CompassSettings compassSettings) {
    ApplicationManager.getApplication().assertIsDispatchThread();

    final Module module = compassSettings.getModule();
    if (module.isDisposed()) {
      return;
    }

    final List<String> importPaths = compassSettings.getImportPaths();
    final Collection<VirtualFile> libraryRoots = getLibraryRootsFromImportPaths(module, importPaths);

    final ModifiableModelsProvider modelsProvider = ModifiableModelsProvider.SERVICE.getInstance();
    final ModifiableRootModel model = modelsProvider.getModuleModifiableModel(module);
    final LibraryOrderEntry compassLibraryEntry = OrderEntryUtil.findLibraryOrderEntry(model, COMPASS_LIBRARY_NAME);

    if (!libraryRoots.isEmpty()) {
      ApplicationManager.getApplication().runWriteAction(() -> {
        if (compassLibraryEntry != null) {
          final Library compassLibrary = compassLibraryEntry.getLibrary();
          if (compassLibrary != null) {
            fillLibrary(module, compassLibrary, libraryRoots);
          }
          else {
            model.removeOrderEntry(compassLibraryEntry);
            createAndFillLibrary(module, model, libraryRoots);
          }
        }
        else {
          createAndFillLibrary(module, model, libraryRoots);
        }
        modelsProvider.commitModuleModifiableModel(model);
      });
    }
    else {
      disposeModel(modelsProvider, model);
      removeCompassLibraryIfNeeded(module);
    }
  }

  /**
   * Remove attached compass library from given module.
   * Due to updating ModuleModifiableModule should be invoked on EDT.
   */
  public static void removeCompassLibraryIfNeeded(@NotNull Module module) {
    ApplicationManager.getApplication().assertIsDispatchThread();

    if (module.isDisposed()) {
      return;
    }

    final ModifiableModelsProvider modelsProvider = ModifiableModelsProvider.SERVICE.getInstance();
    final ModifiableRootModel model = modelsProvider.getModuleModifiableModel(module);
    final LibraryOrderEntry compassLibraryEntry = OrderEntryUtil.findLibraryOrderEntry(model, COMPASS_LIBRARY_NAME);
    if (compassLibraryEntry != null) {
      ApplicationManager.getApplication().runWriteAction(() -> {
        final Library library = compassLibraryEntry.getLibrary();
        if (library != null) {
          final LibraryTable table = library.getTable();
          if (table != null) {
            table.removeLibrary(library);
            model.removeOrderEntry(compassLibraryEntry);
            modelsProvider.commitModuleModifiableModel(model);
          }
        }
        else {
          modelsProvider.disposeModuleModifiableModel(model);
        }
      });
    }
    else {
      disposeModel(modelsProvider, model);
    }
  }

  /**
   * Converts import paths to library roots.
   * If import path is part of some gem (contains '/gems/') then root of gem will be used as library root.
   *
   * Due to refreshing VFS should be invoked on EDT.
   */
  @NotNull
  private static Collection<VirtualFile> getLibraryRootsFromImportPaths(@NotNull final Module module,
                                                                        @NotNull final List<String> importPaths) {
    final Collection<VirtualFile> libraryRoots = new ArrayList<>();
    ApplicationManager.getApplication().runWriteAction(() -> {
      for (String path : importPaths) {
        final VirtualFile importRoot = VfsUtil.findFileByIoFile(new File(path), true);
        final int indexOfGemsDir = path.lastIndexOf(GEMS_PATH_PART);
        if (indexOfGemsDir >= 0) {
          final int indexOfNextSlash = path.indexOf('/', indexOfGemsDir + GEMS_PATH_PART.length());
          final String gemRootPath = indexOfNextSlash >= 0 ? path.substring(0, indexOfNextSlash) : path;
          final VirtualFile gemRoot = VfsUtil.findFileByIoFile(new File(gemRootPath), true);
          if (gemRoot != null) {
            if (!isGemInstalled(gemRoot.getUrl(), module)) {
              libraryRoots.add(gemRoot);
            }
            continue;
          }
        }
        ContainerUtil.addIfNotNull(libraryRoots, importRoot);
      }
    });
    return libraryRoots;
  }

  public static boolean isGemInstalled(@NotNull String gemNameOrUrl, @NotNull Module module) {
    if (!SassRubyIntegrationHelper.getInstance().isRubyModule(module)) {
      return false;
    }
    final GemInfo gem = GemManager.findGem(module, gemNameOrUrl);
    if (gem != null) {
      return true;
    }
    for (GemInfo gemInfo : GemManager.getAllGems(module)) {
      if(gemInfo.getUrl().equals(gemNameOrUrl)) {
        return true;
      }
    }
    return false;
  }

  private static void disposeModel(@NotNull final ModifiableModelsProvider modelsProvider, @NotNull final ModifiableRootModel model) {
    ApplicationManager.getApplication().runWriteAction(() -> modelsProvider.disposeModuleModifiableModel(model));
  }

  private static void fillLibrary(@NotNull Module module,
                                  @NotNull Library compassLibrary,
                                  @NotNull Collection<? extends VirtualFile> importRoots) {
    ApplicationManager.getApplication().assertWriteAccessAllowed();

    final Library.ModifiableModel libraryModel = compassLibrary.getModifiableModel();
    for (String root : libraryModel.getUrls(OrderRootType.CLASSES)) {
      libraryModel.removeRoot(root, OrderRootType.CLASSES);
    }
    for (VirtualFile importRoot : importRoots) {
      if (!ModuleUtilCore.projectContainsFile(module.getProject(), importRoot, false)) {
        libraryModel.addRoot(importRoot, OrderRootType.CLASSES);
      }
    }
    libraryModel.commit();
  }

  private static LibraryOrderEntry createAndFillLibrary(@NotNull Module module,
                                                        @NotNull ModifiableRootModel model,
                                                        @NotNull Collection<? extends VirtualFile> importRoots) {
    ApplicationManager.getApplication().assertWriteAccessAllowed();

    final LibraryTable libraryTable = LibraryTablesRegistrar.getInstance().getLibraryTable(module.getProject());
    final Library compassLibrary = libraryTable.createLibrary(COMPASS_LIBRARY_NAME);
    fillLibrary(module, compassLibrary, importRoots);
    return model.addLibraryEntry(compassLibrary);
  }

  @NotNull
  public static Configurable createCompassConfigurable(Module module, boolean fullMode) {
    CompassSassExtension extension = getCompassExtension();
    return SassRubyIntegrationHelper.getInstance().isRubyModule(module)
            ? new CompassConfigurableForRubyModule(module, extension, fullMode)
            : new CompassConfigurable(module, extension, fullMode);
  }
}
