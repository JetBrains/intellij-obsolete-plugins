package com.jetbrains.plugins.compass;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupManager;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.*;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiTreeChangeAdapter;
import com.intellij.psi.PsiTreeChangeEvent;
import com.intellij.util.CommonProcessors;
import com.intellij.util.PathUtil;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.messages.MessageBusConnection;
import com.intellij.util.messages.Topic;
import com.intellij.util.ui.update.MergingUpdateQueue;
import com.intellij.util.ui.update.Update;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Listener of compass config files.
 * <p/>
 * Responsible for filling CompassSettings with values that had been set in config file (e.g. import_path, css_directory, etc.).
 *
 * @see <a href="http://compass-style.org/help/tutorials/configuration-reference/">Configuration reference</a> for details.
 * <p/>
 * Listeners should be created for each module that supports Compass.
 */
abstract public class CompassImportPathRegistrationWatcher implements Disposable {
  public static final Topic<ImportPathsListener> IMPORT_PATHS_CHANGED =
    Topic.create("compass import paths changed", ImportPathsListener.class, Topic.BroadcastDirection.TO_PARENT);
  protected final Module myModule;
  private static final int QUEUE_UPDATE_TIMEOUT = 5000;
  @Nullable private MergingUpdateQueue myQueue;

  @NotNull private final CompassConfigParser myConfigParser;
  @NotNull private final Object mySyncObject = new Object();
  private boolean myStarted = false;

  public CompassImportPathRegistrationWatcher(@NotNull final Module module, @NotNull CompassConfigParser configParser) {
    myModule = module;
    myConfigParser = configParser;
  }

  public boolean isStarted() {
    synchronized (mySyncObject) {
      return myStarted;
    }
  }

  private boolean isFileToScan(@Nullable VirtualFile file) {
    final CompassSettings settings = CompassSettings.getInstance(myModule);
    return file != null && settings != null && PathUtil.getFileName(settings.getCompassConfigPath()).equals(file.getName())
           && FileUtil.pathsEqual(settings.getCompassConfigPath(), file.getPath());
  }

  private boolean isEventToScan(@NotNull VirtualFileEvent event) {
    final CompassSettings settings = CompassSettings.getInstance(myModule);
    if (settings != null && event instanceof VirtualFilePropertyEvent
        && VirtualFile.PROP_NAME.equals(((VirtualFilePropertyEvent)event).getPropertyName())) {
      final VirtualFile file = event.getFile();
      final VirtualFile parent = file.getParent();
      if (parent != null) {
        final String configPath = settings.getCompassConfigPath();
        final String configFileName = PathUtil.getFileName(configPath);
        final Object oldName = ((VirtualFilePropertyEvent)event).getOldValue();
        final Object newName = ((VirtualFilePropertyEvent)event).getNewValue();
        return (configFileName.equals(oldName) || configFileName.equals(newName))
               && FileUtil.pathsEqual(PathUtil.getParentPath(configPath), parent.getPath());
      }
    }
    return isFileToScan(event.getFile());
  }

  @NotNull
  private Collection<VirtualFile> getFilesToScan() {
    final CompassSettings settings = CompassSettings.getInstance(myModule);
    if (settings != null) {
      String compassConfigPath = settings.getCompassConfigPath();
      if (!compassConfigPath.trim().isEmpty()) {
        final VirtualFile configFile = VirtualFileManager.getInstance().findFileByUrl(VfsUtilCore.pathToUrl(compassConfigPath));
        return ContainerUtil.createMaybeSingletonList(configFile);
      }
    }
    return Collections.emptyList();
  }

  private void fillWithImportPathsFromConfig(@NotNull Module module, @NotNull VirtualFile file, @NotNull final Collection<String> importPaths) {
    importPaths.addAll(myConfigParser.parse(file, PsiManager.getInstance(module.getProject())).getImportPaths());
  }

  public void scheduleImportPathsRefresh() {
    synchronized (mySyncObject) {
      if (myQueue != null) {
        myQueue.queue(new MyImportPathsScan(myModule));
      }
    }
  }

  public void subscribe(@NotNull ImportPathsListener importPathsListener, final boolean enableFileWatchers) {
    if (myModule.isDisposed()) {
      return;
    }

    synchronized (mySyncObject) {
      if (!myStarted) {
        MessageBusConnection connection = myModule.getProject().getMessageBus().connect(this);
        connection.subscribe(IMPORT_PATHS_CHANGED, importPathsListener);
        myQueue = new MergingUpdateQueue("CompassImportPathsRegistrationWatcher", QUEUE_UPDATE_TIMEOUT, true, null, this).setRestartTimerOnAdd(true);

        myStarted = true;
        StartupManager.getInstance(myModule.getProject()).runWhenProjectIsInitialized(() -> {
          synchronized (mySyncObject) {
            if (!myStarted) return; // already disposed
            if (enableFileWatchers) {
              enableWatchers();
            }
          }

          // update import paths on the first start
          ApplicationManager.getApplication().invokeLater(new MyImportPathsScan(myModule));
        });
      }
    }
  }

  public void stop() {
    synchronized (mySyncObject) {
      myStarted = false;
    }
  }

  @Override
  public void dispose() {
  }

  private void enableWatchers() {
    PsiManager.getInstance(myModule.getProject()).addPsiTreeChangeListener(new MyPsiTreeChangeAdapter(), this);
    VirtualFileManager.getInstance().addVirtualFileListener(new MyVirtualFileListener(), this);
  }

  private void doScanImportPaths(@NotNull Module module) {
    if (module.isDisposed()) {
      return;
    }
    Set<String> importPaths = new HashSet<>();
    final Collection<VirtualFile> filesToScan = getFilesToScan();
    for (VirtualFile file : filesToScan) {
      if (file != null && file.isValid()) {
        fillWithImportPathsFromConfig(module, file, importPaths);
        CompassUtil.runCompassImportsAndProcessPaths(module, file, new CommonProcessors.CollectProcessor<>(importPaths), true);
      }
    }
    if (filesToScan.isEmpty()) {
      CompassUtil.runCompassImportsAndProcessPaths(module, null, new CommonProcessors.CollectProcessor<>(importPaths), true);
    }
    module.getMessageBus().syncPublisher(IMPORT_PATHS_CHANGED).pathsChanged(module, importPaths);
  }


  public interface ImportPathsListener {
    void pathsChanged(@NotNull Module module, @NotNull Set<String> newImportPaths);
  }

  private class MyImportPathsScan extends Update {
    private final Module myModule;

    @Override
    public boolean canEat(Update update) {
      return true;
    }

    MyImportPathsScan(@NotNull Module module) {
      super("Compass import paths scan");
      myModule = module;
    }

    @Override
    public void run() {
      if (myModule.isDisposed()) {
        return;
      }

      final Project project = myModule.getProject();
      DumbService.getInstance(project).runWhenSmart(() -> {
        final String title = "Scanning Compass Import Paths";
        ProgressManager.getInstance().run(new Task.Backgroundable(project, title, false) {
          @Override
          public void run(@NotNull ProgressIndicator indicator) {
            if (myProject == null || myProject.isDisposed()) {
              return;
            }
            doScanImportPaths(myModule);
          }
        });
      });
    }
  }

  private class MyPsiTreeChangeAdapter extends PsiTreeChangeAdapter {
    @Override
    public void childAdded(@NotNull final PsiTreeChangeEvent event) {
      processPsiEvent(event);
    }

    @Override
    public void childRemoved(@NotNull final PsiTreeChangeEvent event) {
      processPsiEvent(event);
    }

    @Override
    public void childReplaced(@NotNull final PsiTreeChangeEvent event) {
      processPsiEvent(event);
    }

    @Override
    public void childMoved(@NotNull final PsiTreeChangeEvent event) {
      processPsiEvent(event);
    }

    @Override
    public void childrenChanged(@NotNull final PsiTreeChangeEvent event) {
      processPsiEvent(event);
    }

    private void processPsiEvent(final PsiTreeChangeEvent event) {
      final PsiFile file = event.getFile();
      if (file != null) {
        if (isFileToScan(file.getVirtualFile())) {
          scheduleImportPathsRefresh();
        }
      }
    }
  }

  private class MyVirtualFileListener implements VirtualFileListener {
    @Override
    public void fileCreated(@NotNull final VirtualFileEvent event) {
      processFileEvent(event);
    }

    @Override
    public void fileDeleted(@NotNull final VirtualFileEvent event) {
      processFileEvent(event);
    }

    @Override
    public void propertyChanged(@NotNull final VirtualFilePropertyEvent event) {
      processFileEvent(event);
    }

    @Override
    public void beforeFileMovement(@NotNull final VirtualFileMoveEvent event) {
      processFileEvent(event);
    }

    @Override
    public void fileMoved(@NotNull final VirtualFileMoveEvent event) {
      processFileEvent(event);
    }

    @Override
    public void fileCopied(@NotNull final VirtualFileCopyEvent event) {
      processFileEvent(event);
    }

    @Override
    public void contentsChanged(@NotNull final VirtualFileEvent event) {
      processFileEvent(event);
    }

    private void processFileEvent(@NotNull final VirtualFileEvent event) {
      if (isEventToScan(event)) {
        scheduleImportPathsRefresh();
      }
    }
  }
}
