package com.intellij.lang.puppet.project;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.newvfs.BulkFileListener;
import com.intellij.openapi.vfs.newvfs.events.VFileContentChangeEvent;
import com.intellij.openapi.vfs.newvfs.events.VFileCreateEvent;
import com.intellij.openapi.vfs.newvfs.events.VFileDeleteEvent;
import com.intellij.openapi.vfs.newvfs.events.VFileEvent;
import com.intellij.openapi.vfs.newvfs.events.VFileMoveEvent;
import com.intellij.openapi.vfs.newvfs.events.VFilePropertyChangeEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

final class PuppetProjectFilesListener implements BulkFileListener {
  private static final Set<String> KEY_FILE_NAMES = Set.of(
    PuppetProjectManager.ENVIRONMENT_META_FILE,
    PuppetProjectManager.PUPPET_FILE,
    PuppetProjectManager.MODULE_META_FILE,
    PuppetModule.FIXTURES_FILE
  );

  private final @NotNull Project myProject;

  PuppetProjectFilesListener(@NotNull Project project) {
    myProject = project;
  }

  private static boolean isOurFile(@NotNull VirtualFile virtualFile) {
    return isOurFile(virtualFile.isDirectory(), virtualFile.getName());
  }

  private static boolean isOurFile(boolean isDirectory, String name) {
    return !isDirectory && KEY_FILE_NAMES.contains(name);
  }

  @Override
  public void after(@NotNull List<? extends @NotNull VFileEvent> events) {
    Set<VirtualFile> dirsToUpdateMetadata = new HashSet<>();
    PuppetProjectManager puppetProjectManager = PuppetProjectManager.getInstance(myProject);
    for (VFileEvent event : events) {
      if (event instanceof VFileContentChangeEvent che) {
        VirtualFile file = che.getFile();
        if (isOurFile(file)) {
          dirsToUpdateMetadata.add(file.getParent());
        }
      }
      else if (event instanceof VFileDeleteEvent de) {
        VirtualFile file = de.getFile();
        if (KEY_FILE_NAMES.contains(file.getName())) {
          dirsToUpdateMetadata.add(file.getParent());
        }
      }
      else if (event instanceof VFileCreateEvent ce) {
        if (isOurFile(ce.isDirectory(), ce.getChildName())) {
          dirsToUpdateMetadata.add(ce.getParent());
        }
        else if (ce.isDirectory()) {
          VirtualFile dependencyOwnerRoot = getPossibleDependencyOwnerRoot(ce.getParent());
          if (dependencyOwnerRoot != null) {
            dirsToUpdateMetadata.add(dependencyOwnerRoot);
          }
        }
      }
      else if (event instanceof VFileMoveEvent || event instanceof VFilePropertyChangeEvent) {
        puppetProjectManager.queueRescanProjectStructure();
        return;
      }
    }
    if (!dirsToUpdateMetadata.isEmpty()) {
      dirsToUpdateMetadata.forEach(root -> {
        puppetProjectManager.scheduleScanRoot(root);
      });
    }
  }

  @Override
  public void before(@NotNull List<? extends @NotNull VFileEvent> events) {
    Set<VirtualFile> rootsToClear = new HashSet<>();
    for (VFileEvent event : events) {
      if (event instanceof VFileDeleteEvent de) {
        VirtualFile file = de.getFile();

        if (file.isDirectory()) {
          for (VirtualFile root : PuppetProjectManager.getInstance(myProject).getModel().getAllRoots()) {
            if (VfsUtilCore.isAncestor(file, root, false)) {
              rootsToClear.add(root);
            }
          }
        }
      }
    }
    if (!rootsToClear.isEmpty()) {
      rootsToClear.forEach(PuppetProjectManager.getInstance(myProject)::clearMetaData);
    }
  }

  /**
   * Searches for explicit module or environment containing passed possibleDependency in it's dependency root
   *
   * @return explicit entity root or null if none
   */
  private @Nullable VirtualFile getPossibleDependencyOwnerRoot(@NotNull VirtualFile possibleDependencyRoot) {
    PuppetEntity<?> containingEntity =
      PuppetProjectManager.getInstance(myProject).findExplicitModuleOrEnvironmentForFile(possibleDependencyRoot);
    if (containingEntity == null || !containingEntity.isValid()) {
      return null;
    }

    for (VirtualFile dependencyRoot : containingEntity.getAllDependenciesRoots()) {
      if (possibleDependencyRoot.equals(dependencyRoot)) {
        return containingEntity.getRoot();
      }
    }
    return null;
  }
}
