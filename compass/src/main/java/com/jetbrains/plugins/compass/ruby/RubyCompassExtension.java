package com.jetbrains.plugins.compass.ruby;

import com.intellij.ProjectTopics;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.roots.ModuleRootEvent;
import com.intellij.openapi.roots.ModuleRootListener;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.messages.MessageBusConnection;
import com.jetbrains.plugins.compass.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.ruby.gem.GemInfo;
import org.jetbrains.plugins.ruby.gem.GemManager;
import org.jetbrains.plugins.ruby.gem.util.GemSearchUtil;
import org.jetbrains.plugins.sass.extensions.SassRubyIntegrationHelper;

import java.util.Collection;
import java.util.HashSet;

public class RubyCompassExtension extends CompassSassExtension {
  @Override
  protected boolean isAvailableInModule(@NotNull Module module) {
    // should check RubyPlugin for tests in intellij.idea.ultimate.tests.main
    if (!module.isDisposed() && SassRubyIntegrationHelper.getInstance().hasRubyPlugin()) {
      CompassSettings compassSettings = CompassSettings.getInstance(module);
      return compassSettings != null && compassSettings.isCompassSupportEnabled();
    }
    return false;
  }

  @Override
  public void startActivity(@NotNull final Module module) {
    if (!module.isDisposed()) {
      MessageBusConnection connection = module.getProject().getMessageBus().connect(module);
      connection.subscribe(ProjectTopics.PROJECT_ROOTS, new ModuleRootListener() {
        @Override
        public void rootsChanged(@NotNull final ModuleRootEvent event) {
          enableWatcherIfNeeded(module);
        }
      });

      enableWatcherIfNeeded(module);
    }
  }

  @Override
  public void stopActivity(@NotNull Module module) {
    CompassImportPathRegistrationWatcher watcher = CompassImportPathRegistrationWatcherRM.getInstance(module);
    if (watcher != null) {
      watcher.stop();
    }
    stopDefaultActivity(module);
  }

  private void enableWatcherIfNeeded(@NotNull Module module) {
    if (!module.isDisposed()) {
      if (SassRubyIntegrationHelper.getInstance().isRubyModule(module)) {
        stopDefaultActivity(module);
        if (GemSearchUtil.findGemEx(module, getName()) != null) {
          startRubyActivity(module);
        }
        else {
          stopRubyActivity(module);
          CompassSettings compassSettings = CompassSettings.getInstance(module);
          if (compassSettings != null && compassSettings.isCompassSupportEnabled()) {
            compassSettings.resetEnabledFlag();
          }
        }
      }
      else {
        stopRubyActivity(module);
        startDefaultActivity(module);
      }
    }
  }

  private void startRubyActivity(@NotNull Module module) {
    CompassImportPathRegistrationWatcher watcher = CompassImportPathRegistrationWatcherRM.getInstance(module);
    if (watcher != null) {
      fillCompassSettings(module);
      final CompassSettings settings = CompassSettings.getInstance(module);
      if (settings != null && settings.isCompassSupportEnabled()) {
        watcher.subscribe(this, !settings.getCompassConfigPath().isEmpty());
      }
    }
  }

  private void startDefaultActivity(@NotNull Module module) {
    super.startActivity(module);
  }

  private static void stopRubyActivity(@NotNull Module module) {
    CompassImportPathRegistrationWatcher watcher = CompassImportPathRegistrationWatcherRM.getInstance(module);
    if (watcher != null && watcher.isStarted()) {
      watcher.stop();
    }
  }

  private void stopDefaultActivity(@NotNull Module module) {
    super.stopActivity(module);
  }

  private void fillCompassSettings(@NotNull Module module) {
    final CompassSettings settings = CompassSettings.getInstance(module);
    if (settings != null) {
      final GemInfo gem = GemSearchUtil.findGemEx(module, getName());
      if (gem != null) {
        final VirtualFile gemFile = gem.getFile();
        if (gemFile != null) {
          final VirtualFile compassFile = gemFile.findFileByRelativePath(CompassUtil.COMPASS_EXECUTABLE_RELATIVE_PATH);
          if (compassFile != null && GemUtil.isValidExecutableFile(compassFile)) {
            settings.setCompassExecutableFilePath(compassFile.getPath());
          }
        }
      }
    }
  }

  @NotNull
  @Override
  public Collection<? extends VirtualFile> getStylesheetsRoots(@NotNull Module module) {
    Collection<VirtualFile> result = new HashSet<>(super.getStylesheetsRoots(module));
    for (GemInfo gemInfo : GemManager.getAllGems(module)) {
      VirtualFile gemDirectory = gemInfo.getFile();
      if (gemDirectory != null) {
        VirtualFile stylesheetsDirectory = gemDirectory.findChild("stylesheets");
        if (stylesheetsDirectory != null) {
          result.add(stylesheetsDirectory);
        }
        VirtualFile sassDirectory = gemDirectory.findChild("sass");
        if (sassDirectory != null) {
          result.add(sassDirectory);
        }
      }
    }
    return result;
  }
}
