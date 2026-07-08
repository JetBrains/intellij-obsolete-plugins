package com.intellij.lang.puppet.project.roots;

import com.intellij.concurrency.ConcurrentCollectionFactory;
import com.intellij.lang.puppet.project.PuppetProjectModel;
import com.intellij.openapi.fileTypes.FileTypeRegistry;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.newvfs.NewVirtualFile;
import com.intellij.openapi.vfs.newvfs.events.VFileEvent;
import com.intellij.util.containers.IntObjectMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Based on com.intellij.openapi.roots.impl.RootIndex
 */
public class PuppetRootIndex {
  private final InfoCache myInfoCache = new InfoCache();
  private static final FileTypeRegistry ourFileTypes = FileTypeRegistry.getInstance();

  public PuppetRootIndex(@NotNull Project project) {
    ProjectFileIndex fileIndex = ProjectFileIndex.getInstance(project);
    for (VirtualFile root : PuppetProjectModel.getInstance(project).getAllRoots()) {
      cacheInfos(root, root, calcDirectoryInfo(fileIndex, root));
    }
  }

  private static @NotNull PuppetDirectoryInfo calcDirectoryInfo(ProjectFileIndex fileIndex, @NotNull VirtualFile virtualFile) {
    if (!fileIndex.isInContent(virtualFile) && !fileIndex.isInLibrary(virtualFile)) {
      return PuppetDirectoryInfo.UNAVAILABLE;
    }
    return new PuppetDirectoryInfo(virtualFile);
  }

  boolean resetOnEvents(@NotNull List<? extends VFileEvent> events) {
    for (VFileEvent event : events) {
      VirtualFile file = event.getFile();
      if (file == null || file.isDirectory()) {
        return true;
      }
    }
    return false;
  }

  public @NotNull PuppetDirectoryInfo getInfoForFile(@NotNull VirtualFile file) {

    if (!file.isValid()) {
      return PuppetDirectoryInfo.UNAVAILABLE;
    }
    VirtualFile dir;
    if (!file.isDirectory()) {
      PuppetDirectoryInfo info = myInfoCache.getCachedInfo(file);
      if (info != null) {
        return info;
      }
      if (ourFileTypes.isFileIgnored(file)) {
        return PuppetDirectoryInfo.UNAVAILABLE;
      }
      dir = file.getParent();
    }
    else {
      dir = file;
    }

    int count = 0;
    for (VirtualFile root = dir; root != null; root = root.getParent()) {
      if (++count > 1000) {
        throw new IllegalStateException("Possible loop in tree, started at " + dir.getName());
      }
      PuppetDirectoryInfo info = myInfoCache.getCachedInfo(root);
      if (info != null) {
        if (!dir.equals(root)) {
          cacheInfos(dir, root, info);
        }
        return info;
      }

      if (ourFileTypes.isFileIgnored(root)) {
        return cacheInfos(dir, root, PuppetDirectoryInfo.UNAVAILABLE);
      }
    }
    return cacheInfos(dir, null, PuppetDirectoryInfo.UNAVAILABLE);
  }

  private @NotNull PuppetDirectoryInfo cacheInfos(VirtualFile dir, @Nullable VirtualFile stopAt, @NotNull PuppetDirectoryInfo info) {
    while (dir != null) {
      myInfoCache.cacheInfo(dir, info);
      if (dir.equals(stopAt)) {
        break;
      }
      dir = dir.getParent();
    }
    return info;
  }

  private static class InfoCache {
    // Upsource can't use int-mapping because different files may have the same id there
    private final IntObjectMap<PuppetDirectoryInfo> myInfoCache =
      ConcurrentCollectionFactory.createConcurrentIntObjectMap();

    public void cacheInfo(@NotNull VirtualFile dir, @NotNull PuppetDirectoryInfo info) {
      myInfoCache.put(((NewVirtualFile)dir).getId(), info);
    }

    public PuppetDirectoryInfo getCachedInfo(@NotNull VirtualFile dir) {
      return myInfoCache.get(((NewVirtualFile)dir).getId());
    }
  }
}
