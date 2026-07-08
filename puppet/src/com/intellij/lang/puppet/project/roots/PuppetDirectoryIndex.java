package com.intellij.lang.puppet.project.roots;

import com.intellij.lang.puppet.project.PuppetProjectListener;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileTypes.FileTypeEvent;
import com.intellij.openapi.fileTypes.FileTypeListener;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModuleRootEvent;
import com.intellij.openapi.roots.ModuleRootListener;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.openapi.vfs.newvfs.BulkFileListener;
import com.intellij.openapi.vfs.newvfs.NewVirtualFile;
import com.intellij.openapi.vfs.newvfs.events.VFileEvent;
import com.intellij.util.messages.MessageBusConnection;
import org.jetbrains.annotations.NotNull;

import java.util.List;

import static com.intellij.lang.puppet.project.PuppetProjectManager.PUPPET_PROJECT_TOPIC;

/**
 * Based on com.intellij.openapi.roots.impl.DirectoryIndexImpl
 */
public class PuppetDirectoryIndex implements Disposable {
  private static final Logger LOG = Logger.getInstance(PuppetDirectoryIndex.class);
  private final Project myProject;
  private final MessageBusConnection myConnection;
  private volatile boolean myDisposed;
  private volatile PuppetRootIndex myRootIndex;

  public PuppetDirectoryIndex(Project project) {
    myProject = project;
    myConnection = project.getMessageBus().connect(this);
    subscribeToFileChanges();
  }

  @Override
  public void dispose() {
    myDisposed = true;
    myRootIndex = null;
  }

  private void subscribeToFileChanges() {
    myConnection.subscribe(FileTypeManager.TOPIC, new FileTypeListener() {
      @Override
      public void fileTypesChanged(@NotNull FileTypeEvent event) {
        myRootIndex = null;
      }
    });

    myConnection.subscribe(ModuleRootListener.TOPIC, new ModuleRootListener() {
      @Override
      public void rootsChanged(@NotNull ModuleRootEvent event) {
        myRootIndex = null;
      }
    });

    myConnection.subscribe(VirtualFileManager.VFS_CHANGES, new BulkFileListener() {
      @Override
      public void after(@NotNull List<? extends @NotNull VFileEvent> events) {
        PuppetRootIndex rootIndex = myRootIndex;
        if (rootIndex != null && rootIndex.resetOnEvents(events)) {
          myRootIndex = null;
        }
      }
    });
    myConnection.subscribe(PUPPET_PROJECT_TOPIC, new PuppetProjectListener() {
      @Override
      public void projectUpdated() {
        myRootIndex = null;
      }
    });
  }

  protected void dispatchPendingEvents() {
    myConnection.deliverImmediately();
  }

  private @NotNull PuppetRootIndex getRootIndex() {
    PuppetRootIndex rootIndex = myRootIndex;
    if (rootIndex == null) {
      myRootIndex = rootIndex = new PuppetRootIndex(myProject);
    }
    return rootIndex;
  }

  public @NotNull PuppetDirectoryInfo getInfoForFile(@NotNull VirtualFile file) {
    checkAvailability();
    dispatchPendingEvents();

    if (!(file instanceof NewVirtualFile)) return PuppetDirectoryInfo.UNAVAILABLE;

    return getRootIndex().getInfoForFile(file);
  }

  private void checkAvailability() {
    if (myDisposed) {
      ProgressManager.checkCanceled();
      LOG.error("Directory index is already disposed for " + myProject);
    }
  }

  public static PuppetDirectoryIndex getInstance(Project project) {
    if (project.isDefault()) {
      throw new AssertionError("Must not call PuppetDirectoryIndex for default project");
    }
    return project.getService(PuppetDirectoryIndex.class);
  }
}
