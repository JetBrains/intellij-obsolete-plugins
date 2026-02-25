// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package org.jetbrains.plugins.groovy.mvc;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.project.ModuleListener;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModuleRootEvent;
import com.intellij.openapi.roots.ModuleRootListener;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.startup.StartupActivity;
import com.intellij.openapi.startup.StartupManager;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.SimpleModificationTracker;
import com.intellij.openapi.util.Trinity;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileEvent;
import com.intellij.openapi.vfs.VirtualFileListener;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.openapi.vfs.VirtualFileMoveEvent;
import com.intellij.openapi.vfs.VirtualFilePropertyEvent;
import com.intellij.openapi.vfs.impl.BulkVirtualFileListenerAdapter;
import com.intellij.openapi.vfs.pointers.VirtualFilePointer;
import com.intellij.openapi.vfs.pointers.VirtualFilePointerListener;
import com.intellij.openapi.vfs.pointers.VirtualFilePointerManager;
import com.intellij.util.concurrency.AppExecutorUtil;
import com.intellij.util.concurrency.ThreadingAssertions;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.messages.MessageBusConnection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.TestOnly;
import org.jetbrains.plugins.grails.config.GrailsFramework;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service(Service.Level.PROJECT)
public final class MvcModuleStructureSynchronizer implements Disposable {
  private final Set<Pair<Object, SyncAction>> myOrders = new LinkedHashSet<>();
  private final Project myProject;

  private Set<VirtualFile> myPluginRoots = Collections.emptySet();

  private boolean myOutOfModuleDirectoryCreatedActionAdded;

  @SuppressWarnings("StaticNonFinalField") public static boolean ourGrailsTestFlag;

  private final SimpleModificationTracker myModificationTracker = new SimpleModificationTracker();

  public MvcModuleStructureSynchronizer(@NotNull Project project) {
    myProject = project;
  }

  @Override
  public void dispose() {
    // noop
  }

  public static @NotNull MvcModuleStructureSynchronizer getInstance(@NotNull Project project) {
    return project.getService(MvcModuleStructureSynchronizer.class);
  }

  static final class MyPostStartUpActivity implements StartupActivity.DumbAware {
    @Override
    public void runActivity(@NotNull Project project) {
      ApplicationManager.getApplication().invokeLater(() -> getInstance(project).projectOpened(), project.getDisposed());
    }
  }

  private void projectOpened() {
    synchronized (myOrders) {
      Project project = myProject;
      myOrders.add(new Pair<>(project, SyncAction.UpdateProjectStructure));
      myOrders.add(new Pair<>(project, SyncAction.UpgradeFramework));
    }
    scheduleRunActions();

    addListeners();
  }

  public @NotNull SimpleModificationTracker getFileAndRootsModificationTracker() {
    return myModificationTracker;
  }

  private void addListeners() {
    MessageBusConnection connection = myProject.getMessageBus().connect(this);
    for (String rootPath : MvcWatchedRootProvider.doGetRootsToWatch(myProject)) {
      VirtualFilePointerManager.getInstance().createDirectoryPointer(VfsUtilCore.pathToUrl(rootPath), true, this, new VirtualFilePointerListener() {
        @Override
        public void validityChanged(@NotNull VirtualFilePointer @NotNull [] pointers) {
          myModificationTracker.incModificationCount();
          synchronized (myOrders) {
            myOrders.add(new Pair<>(myProject, SyncAction.SyncLibrariesInPluginsModule));
          }
          scheduleRunActions();
        }
      });
    }

    connection.subscribe(ModuleRootListener.TOPIC, new ModuleRootListener() {
      @Override
      public void rootsChanged(@NotNull ModuleRootEvent event) {
        myModificationTracker.incModificationCount();
        synchronized (myOrders) {
          Project project = myProject;
          myOrders.add(new Pair<>(project, SyncAction.UpgradeFramework));
          myOrders.add(new Pair<>(project, SyncAction.UpdateProjectStructure));
        }
        scheduleRunActions();
      }
    });

    connection.subscribe(ModuleListener.TOPIC, new ModuleListener() {
      @Override
      public void modulesAdded(@NotNull Project project, @NotNull List<? extends Module> modules) {
        synchronized (myOrders) {
          myOrders.add(new Pair<>(project, SyncAction.UpdateProjectStructure));
        }
        scheduleRunActions();
      }
    });

    connection.subscribe(VirtualFileManager.VFS_CHANGES, new BulkVirtualFileListenerAdapter(new VirtualFileListener() {
      final ProjectFileIndex myFileIndex = ProjectFileIndex.getInstance(myProject);

      @Override
      public void fileCreated(@NotNull VirtualFileEvent event) {
        VirtualFile file = event.getFile();
        if (!myFileIndex.isInContent(file)) {
          return;
        }

        myModificationTracker.incModificationCount();

        String fileName = event.getFileName();
        if (MvcModuleStructureUtil.APPLICATION_PROPERTIES.equals(fileName) || isApplicationDirectoryName(fileName)) {
          queue(SyncAction.UpdateProjectStructure, file);
        }
        else if (isLibDirectory(file) || isLibDirectory(event.getParent())) {
          queue(SyncAction.UpdateProjectStructure, file);
        }
        else {
          if (!myProject.isInitialized()) return;

          Module module = ProjectRootManager.getInstance(myProject).getFileIndex().getModuleForFile(file);

          if (module == null) { // Maybe it is creation of a plugin in plugin directory.
            if (file.isDirectory()) {
              if (myPluginRoots.contains(file.getParent())) {
                queue(SyncAction.UpdateProjectStructure, myProject);
                return;
              }

              if (!myOutOfModuleDirectoryCreatedActionAdded) {
                queue(SyncAction.OutOfModuleDirectoryCreated, myProject);
                myOutOfModuleDirectoryCreatedActionAdded = true;
              }
            }
          }
        }
      }

      @Override
      public void fileDeleted(@NotNull VirtualFileEvent event) {
        VirtualFile file = event.getFile();

        myModificationTracker.incModificationCount();

        if (isLibDirectory(file) || isLibDirectory(event.getParent())) {
          queue(SyncAction.UpdateProjectStructure, file);
        }
      }

      @Override
      public void contentsChanged(@NotNull VirtualFileEvent event) {
        VirtualFile file = event.getFile();
        if (!myFileIndex.isInContent(file)) return;

        String fileName = event.getFileName();
        if (MvcModuleStructureUtil.APPLICATION_PROPERTIES.equals(fileName)) {
          queue(SyncAction.UpdateProjectStructure, file);
        }
      }

      @Override
      public void fileMoved(@NotNull VirtualFileMoveEvent event) {
        if (!myFileIndex.isInContent(event.getFile())) return;
        myModificationTracker.incModificationCount();
      }

      @Override
      public void propertyChanged(@NotNull VirtualFilePropertyEvent event) {
        if (!myFileIndex.isInContent(event.getFile())) return;
        if (VirtualFile.PROP_NAME.equals(event.getPropertyName())) {
          myModificationTracker.incModificationCount();
        }
      }
    }));
  }

  private static boolean isApplicationDirectoryName(@NotNull String fileName) {
    return GrailsFramework.getInstance().getApplicationDirectoryName().equals(fileName);
  }

  private static boolean isLibDirectory(@Nullable VirtualFile file) {
    return file != null && "lib".equals(file.getName());
  }

  public void queue(@NotNull SyncAction action, @NotNull Object on) {
    ThreadingAssertions.assertEventDispatchThread();
    if (myProject.isDisposed()) {
      return;
    }

    synchronized (myOrders) {
      myOrders.add(Pair.create(on, action));
    }
    StartupManager.getInstance(myProject).runAfterOpened(this::scheduleRunActions);
  }

  private void scheduleRunActions() {
    if (ApplicationManager.getApplication().isUnitTestMode()) {
      if (ourGrailsTestFlag && !myProject.isInitialized()) {
        runActions(computeRawActions());
      }
      return;
    }

    ReadAction
      .nonBlocking(() -> computeRawActions())
      .expireWith(this)
      .coalesceBy(this)
      .finishOnUiThread(ModalityState.nonModal(), this::runActions)
      .submit(AppExecutorUtil.getAppExecutorService());
  }

  private @NotNull Set<Pair<Object, SyncAction>> takeOrderSnapshot() {
    synchronized (myOrders) {
      return new LinkedHashSet<>(myOrders);
    }
  }

  private @NotNull List<Module> determineModuleBySyncActionObject(@NotNull Object o) {
    if (o instanceof Module) {
      return Collections.singletonList((Module)o);
    }
    if (o instanceof Project) {
      return Arrays.asList(ModuleManager.getInstance((Project)o).getModules());
    }
    if (o instanceof VirtualFile file) {
      if (file.isValid()) {
        Module module = ModuleUtilCore.findModuleForFile(file, myProject);
        if (module == null) {
          return Collections.emptyList();
        }

        return Collections.singletonList(module);
      }
    }
    return Collections.emptyList();
  }

  @TestOnly
  public static void forceUpdateProject(@NotNull Project project) {
    MvcModuleStructureSynchronizer instance = getInstance(project);
    instance.getFileAndRootsModificationTracker().incModificationCount();
    instance.runActions(instance.computeRawActions());
  }

  private void runActions(@NotNull Set<? extends Trinity<Module, SyncAction, GrailsFramework>> actions) {
    try {
      for (Trinity<Module, SyncAction, GrailsFramework> rawAction : actions) {
        Module module = rawAction.first;
        if (module.isDisposed()) {
          continue;
        }
        rawAction.second.doAction(module, rawAction.third);
      }
    }
    finally {
      // if there were any actions added during performSyncAction, clear them too
      // all needed actions are already added to buffer and have thus been performed
      // otherwise you may get repetitive 'run create-app?' questions
      synchronized (myOrders) {
        myOrders.clear();
      }
    }
  }

  private @NotNull Set<Trinity<Module, SyncAction, GrailsFramework>> computeRawActions() {
    Set<Pair<Object, SyncAction>> actions = takeOrderSnapshot();
    //get module by object and kill duplicates
    Set<Trinity<Module, SyncAction, GrailsFramework>> rawActions = new LinkedHashSet<>();
    for (Pair<Object, SyncAction> pair : actions) {
      for (Module module : determineModuleBySyncActionObject(pair.first)) {
        if (!module.isDisposed()) {
          GrailsFramework framework = GrailsFramework.getInstance(module);
          if (framework != null && !framework.isAuxModule(module)) {
            rawActions.add(Trinity.create(module, pair.second, framework));
          }
        }
      }
    }
    return rawActions;
  }

  public enum SyncAction {
    SyncLibrariesInPluginsModule {
      @Override
      void doAction(@NotNull Module module, @NotNull GrailsFramework framework) {
        if (MvcModuleStructureUtil.isEnabledStructureUpdate()) {
          framework.syncSdkAndLibrariesInPluginsModule(module);
        }
      }
    },

    UpgradeFramework {
      @Override
      void doAction(@NotNull Module module, @NotNull GrailsFramework framework) {
        framework.upgradeFramework(module);
      }
    },

    UpdateProjectStructure {
      @Override
      void doAction(@NotNull Module module, @NotNull GrailsFramework framework) {
        framework.updateProjectStructure(module);
      }
    },

    OutOfModuleDirectoryCreated {
      @Override
      void doAction(@NotNull Module module, @NotNull GrailsFramework framework) {
        Project project = module.getProject();
        MvcModuleStructureSynchronizer mvcModuleStructureSynchronizer = getInstance(project);

        if (mvcModuleStructureSynchronizer.myOutOfModuleDirectoryCreatedActionAdded) {
          mvcModuleStructureSynchronizer.myOutOfModuleDirectoryCreatedActionAdded = false;

          Set<VirtualFile> roots = new HashSet<>();

          for (String rootPath : MvcWatchedRootProvider.doGetRootsToWatch(project)) {
            ContainerUtil.addIfNotNull(roots, LocalFileSystem.getInstance().findFileByPath(rootPath));
          }

          if (!roots.equals(mvcModuleStructureSynchronizer.myPluginRoots)) {
            mvcModuleStructureSynchronizer.myPluginRoots = roots;
            ApplicationManager.getApplication().invokeLater(() -> mvcModuleStructureSynchronizer.queue(UpdateProjectStructure, project));
          }
        }
      }
    };

    abstract void doAction(@NotNull Module module, @NotNull GrailsFramework framework);
  }
}
