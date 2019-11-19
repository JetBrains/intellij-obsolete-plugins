package com.jetbrains.plugins.compass;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.sass.extensions.SassExtension;
import org.jetbrains.plugins.sass.extensions.SassExtensionFunctionInfo;
import org.jetbrains.plugins.sass.extensions.SassExtensionFunctionInfoImpl;
import org.jetbrains.plugins.sass.extensions.SassRubyIntegrationHelper;

import java.util.*;

import static com.intellij.openapi.util.Pair.pair;

public class CompassSassExtension extends SassExtension implements CompassImportPathRegistrationWatcher.ImportPathsListener {
  private static final Set<String> APPROPRIATE_MODULE_NAMES = ContainerUtil.newHashSet(
    "Compass::SassExtensions::Functions",
    "Compass::Core::SassExtensions::Functions");

  private static final Map<String, SassExtensionFunctionInfo> CUSTOM_FUNCTIONS = ContainerUtil.newHashMap(
    pair("_webkit", new SassExtensionFunctionInfoImpl("-webkit", "$arg, ...", "This is a shortcut for calling prefix(-webkit, $arg, ...).", "compass", null)),
    pair("_o", new SassExtensionFunctionInfoImpl("-o", "$arg, ...", "This is a shortcut for calling prefix(-o, $arg, ...).", "compass", null)),
    pair("_ms", new SassExtensionFunctionInfoImpl("-ms", "$arg, ...", "This is a shortcut for calling prefix(-ms, $arg, ...).", "compass", null)),
    pair("_svg", new SassExtensionFunctionInfoImpl("-svg", "$arg, ...", "This is a shortcut for calling prefix(-svg, $arg, ...).", "compass", null)),
    pair("_pie", new SassExtensionFunctionInfoImpl("-pie", "$arg, ...", "This is a shortcut for calling prefix(-pie, $arg, ...).", "compass", null)),
    pair("_css2", new SassExtensionFunctionInfoImpl("-css2", "$arg, ...", "This is a shortcut for calling prefix(-css2, $arg, ...).", "compass", null)),
    pair("_owg", new SassExtensionFunctionInfoImpl("-owg", "$arg, ...", "This is a shortcut for calling prefix(-owg, $arg, ...).", "compass", null)),
    pair("_moz", new SassExtensionFunctionInfoImpl("-moz", "$arg, ...", "This is a shortcut for calling prefix(-moz, $arg, ...).", "compass", null)));

  @Override
  public String getName() {
    return CompassUtil.COMPASS_GEM_NAME;
  }

  @Override
  protected boolean isAvailableInModule(@NotNull Module module) {
    if (module.isDisposed() || SassRubyIntegrationHelper.getInstance().hasRubyPlugin()) {
      return false;
    }
    final CompassSettings compassSettings = CompassSettings.getInstance(module);
    return compassSettings != null && compassSettings.isCompassSupportEnabled();
  }

  public void startActivity(@NotNull final Module module) {
    if (!module.isDisposed()) {
      final CompassSettings settings = CompassSettings.getInstance(module);
      if (settings != null && settings.isCompassSupportEnabled()) {
        CompassImportPathRegistrationWatcher compassImportPathsWatcher = CompassImportPathRegistrationWatcherImpl.getInstance(module);
        if (compassImportPathsWatcher != null) {
          compassImportPathsWatcher.subscribe(this, !settings.getCompassConfigPath().isEmpty());
        }
      }
    }
  }

  public void stopActivity(@NotNull final Module module) {
    final CompassImportPathRegistrationWatcher watcher = CompassImportPathRegistrationWatcherImpl.getInstance(module);
    if (watcher != null) {
      final boolean wasStarted = watcher.isStarted();
      watcher.stop();
      if (wasStarted) {
        ApplicationManager.getApplication().invokeLater(() -> CompassUtil.removeCompassLibraryIfNeeded(module), ModalityState.NON_MODAL);
      }
    }
  }

  @NotNull
  @Override
  public Collection<SassExtensionFunctionInfo> getCustomFunctions() {
    return CUSTOM_FUNCTIONS.values();
  }

  @Nullable
  @Override
  public SassExtensionFunctionInfo findCustomFunctionByName(@NotNull String name) {
    return CUSTOM_FUNCTIONS.get(name);
  }

  @NotNull
  @Override
  public Set<String> getRubyModulesWithFunctionExtensions() {
    return APPROPRIATE_MODULE_NAMES;
  }

  @Override
  public void pathsChanged(@NotNull Module module, @NotNull Set<String> newImportPaths) {
    if (!module.isDisposed()) {
      final CompassSettings compassSettings = CompassSettings.getInstance(module);
      if (!newImportPaths.equals(new HashSet<>(compassSettings.getImportPaths()))) {
        compassSettings.setImportPaths(new ArrayList<>(newImportPaths));
        ApplicationManager.getApplication().invokeLater(() -> CompassUtil.updateCompassLibraries(compassSettings));
      }
    }
  }

  @NotNull
  @Override
  public Collection<? extends VirtualFile> getStylesheetsRoots(@NotNull Module module) {
    if (!module.isDisposed()) {
      final CompassSettings compassSettings = CompassSettings.getInstance(module);
      if (compassSettings != null && compassSettings.isCompassSupportEnabled()) {
        final List<String> importPaths = compassSettings.getImportPaths();
        if (!importPaths.isEmpty()) {
          final Collection<VirtualFile> result = new LinkedList<>();
          final VirtualFileManager virtualFileManager = VirtualFileManager.getInstance();
          for (final String path : importPaths) {
            final VirtualFile vfile = virtualFileManager.findFileByUrl(VfsUtilCore.pathToUrl(path));
            if (vfile != null) {
              result.add(vfile);
            }
          }
          return result;
        }
      }
    }
    return Collections.emptyList();
  }
}