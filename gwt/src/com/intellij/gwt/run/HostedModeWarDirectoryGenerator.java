package com.intellij.gwt.run;

import com.intellij.gwt.GwtBundle;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.progress.PerformInBackgroundOption;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Clock;
import com.intellij.openapi.util.Couple;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.StatusBar;
import com.intellij.openapi.wm.WindowManager;
import com.intellij.util.containers.CollectionFactory;
import com.intellij.util.containers.FileCollectionFactory;
import com.intellij.util.text.DateFormatUtil;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class HostedModeWarDirectoryGenerator {
  private static final boolean CLEAN_FILES_CREATED_BY_DEV_MODE = Boolean.parseBoolean(System.getProperty("idea.gwt.clean.files.created.by.dev.mode", "true"));
  private static final Logger LOG = Logger.getInstance(HostedModeWarDirectoryGenerator.class);
  private final File myOutputRoot;
  private final File myCacheFile;
  private final List<Couple<File>> myFilesToCopy = new ArrayList<>();

  public HostedModeWarDirectoryGenerator(File outputRoot, File cacheFile) {
    myOutputRoot = getCanonicalFile(outputRoot);
    myCacheFile = cacheFile;
  }

  private static File getCanonicalFile(File file) {
    try {
      return file.getCanonicalFile();
    }
    catch (IOException e) {
      LOG.info(e);
      return file;
    }
  }

  public void addFile(VirtualFile file, @NonNls String relativePath) {
    final File root = VfsUtilCore.virtualToIoFile(file);
    if (root.exists()) {
      String normalizedPath = StringUtil.trimStart(FileUtil.toSystemDependentName(relativePath), File.separator);
      addFile(root, normalizedPath, myOutputRoot.getAbsolutePath());
    }
  }

  private void addFile(File file, String relativePath, final String outputRoot) {
    final File[] files = file.listFiles();
    if (files != null) {
      for (File child : files) {
        addFile(child, relativePath.isEmpty()? child.getName() : relativePath + File.separator + child.getName(), outputRoot);
      }
    }
    else {
      // is file
      File target = new File(outputRoot, relativePath);
      myFilesToCopy.add(Couple.of(file, target));
    }
  }

  public void generate(@NotNull Project project) {
    new Task.Modal(project, GwtBundle.message("task.title.preparing.war.directory.for.gwt.dev.mode"), false) {
      @Override
      public void run(@NotNull ProgressIndicator indicator) {
        doGenerate(indicator, CLEAN_FILES_CREATED_BY_DEV_MODE);
      }
    }.queue();
  }

  private boolean doGenerate(@NotNull ProgressIndicator indicator, final boolean cleanFilesGeneratedByDevModeProcess) {
    HostedModeWarDirectoryCache cache = new HostedModeWarDirectoryCache(myCacheFile);
    cache.load();

    boolean updated = false;
    try {
      final Set<String> toDelete = CollectionFactory.createFilePathSet();
      indicator.setText(GwtBundle.message("progress.indicator.processing.files"));

      if (cleanFilesGeneratedByDevModeProcess) {
        final Set<File> targetFiles = FileCollectionFactory.createCanonicalFileSet();
        for (Couple<File> pair : myFilesToCopy) {
          targetFiles.add(pair.getSecond());
        }
        FileUtil.processFilesRecursively(myOutputRoot, file -> {
          if (file.isFile() && !targetFiles.contains(file)) {
            toDelete.add(file.getAbsolutePath());
          }
          return true;
        });
      }
      else {
        //delete only files copied by IDEA
        toDelete.addAll(cache.getTargetPaths());
        int i = 0;
        for (Couple<File> pair : myFilesToCopy) {
          indicator.setFraction(((double)i++) / myFilesToCopy.size());
          indicator.checkCanceled();
          final File source = pair.getFirst();
          final String targetPath = cache.getTargetPath(source.getPath());
          if (targetPath != null && source.exists()) {
            toDelete.remove(targetPath);
          }
        }
      }

      int i = 0;
      indicator.setText(GwtBundle.message("progress.indicator.deleting.obsolete.files"));
      for (String targetPath : toDelete) {
        indicator.setFraction(((double)i++)/toDelete.size());
        indicator.checkCanceled();
        LOG.debug("Deleting " + targetPath);
        updated = true;
        FileUtil.delete(new File(targetPath));
      }

      Set<String> removeFromCache = new HashSet<>(cache.getSourcePaths());
      Set<String> targetPaths = CollectionFactory.createFilePathSet();
      i = 0;
      indicator.setText(GwtBundle.message("progress.indicator.copying.files"));
      for (Couple<File> pair : myFilesToCopy) {
        indicator.setFraction(((double)i++) / myFilesToCopy.size());
        indicator.checkCanceled();
        final File source = pair.getFirst();
        final File target = pair.getSecond();
        if (!source.exists() || !targetPaths.add(target.getAbsolutePath())) {
          continue;
        }

        final String sourcePath = source.getPath();
        final String targetPath = target.getAbsolutePath();
        long timestamp = cache.getTimestamp(sourcePath);
        final long actualTimestamp = source.lastModified();
        if (timestamp != actualTimestamp || !targetPath.equals(cache.getTargetPath(sourcePath)) || !target.exists()) {
          FileUtil.copy(source, target);
          updated = true;
          LOG.debug("Copying " + sourcePath + " to " + targetPath);
          cache.updateTimestamp(sourcePath, actualTimestamp, targetPath);
        }
        removeFromCache.remove(sourcePath);
      }
      cache.remove(removeFromCache);
      cache.save();
    }
    catch (IOException e) {
      LOG.info(e);
    }

    return updated;
  }

  public void updateResources(final Project project, final @Nullable String configurationName) {
    WriteAction.run(FileDocumentManager.getInstance()::saveAllDocuments);
    new Task.Backgroundable(project, GwtBundle.message("task.title.updating.gwt.dev.mode.resources"), true, PerformInBackgroundOption.ALWAYS_BACKGROUND) {
      @Override
      public void run(@NotNull ProgressIndicator indicator) {
        final boolean updated = doGenerate(indicator, false);
        ApplicationManager.getApplication().invokeLater(() -> {
          String time = DateFormatUtil.formatTime(Clock.getTime());
          final StatusBar statusBar = WindowManager.getInstance().getStatusBar(project);
          if (statusBar != null && configurationName != null) {
            statusBar.setInfo(GwtBundle.message("status.bar.text.gwt.dev.mode.resources.for.0.updated", configurationName, time, updated ? 0 : 1));
          }
        });
      }
    }.queue();
  }
}
