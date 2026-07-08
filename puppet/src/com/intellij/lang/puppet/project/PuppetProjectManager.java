package com.intellij.lang.puppet.project;

import com.intellij.ide.lightEdit.LightEdit;
import com.intellij.ide.projectView.ProjectView;
import com.intellij.lang.puppet.project.meta.PuppetHeadlessModuleMetadata;
import com.intellij.lang.puppet.project.meta.PuppetModuleMetadata;
import com.intellij.lang.puppet.project.roots.PuppetDirectoryIndex;
import com.intellij.lang.puppet.project.roots.PuppetDirectoryInfo;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.application.impl.NonBlockingReadActionImpl;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.search.FilenameIndex;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.ProjectScope;
import com.intellij.ui.treeStructure.ProjectViewUpdateCause;
import com.intellij.util.Processor;
import com.intellij.util.TimeoutUtil;
import com.intellij.util.concurrency.AppExecutorUtil;
import com.intellij.util.concurrency.BoundedTaskExecutor;
import com.intellij.util.concurrency.annotations.RequiresReadLock;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.messages.Topic;
import com.intellij.util.ui.UIUtil;
import com.intellij.util.ui.update.MergingUpdateQueue;
import com.intellij.util.ui.update.Update;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.TestOnly;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class PuppetProjectManager implements Disposable {
  private static final Logger LOG = Logger.getInstance(PuppetProjectManager.class);
  public static final Topic<PuppetProjectListener> PUPPET_PROJECT_TOPIC = new Topic<>("Puppet project change", PuppetProjectListener.class);
  public static final String MODULE_META_FILE = "metadata.json";
  public static final String ENVIRONMENT_META_FILE = "environment.conf";
  public static final String PUPPET_FILE = "Puppetfile";
  private final PuppetProjectModel myModel = new PuppetProjectModel();

  private final @NotNull Project myProject;
  private final @NotNull MergingUpdateQueue myUpdateQueue;
  private final @NotNull MergingUpdateQueue myProjectRescanQueue;
  private final @NotNull BoundedTaskExecutor myUpdateExecutor =
    (BoundedTaskExecutor)AppExecutorUtil.createBoundedApplicationPoolExecutor("Puppet update executor", 1);

  public PuppetProjectManager(@NotNull Project project) {
    myProject = project;

    int updateDelay = ApplicationManager.getApplication().isUnitTestMode() ? 0 : 100;
    myUpdateQueue = new MergingUpdateQueue(
      "Puppet project update notification queue", updateDelay, true, MergingUpdateQueue.ANY_COMPONENT, this);
    myProjectRescanQueue = new MergingUpdateQueue(
      "Rescanning project structure", updateDelay, true, MergingUpdateQueue.ANY_COMPONENT, this);

    myModel.setChangeListener(this::onProjectUpdated);
  }

  public static PuppetProjectManager getInstance(@NotNull Project project) {
    return project.getService(PuppetProjectManager.class);
  }

  @Override
  public void dispose() {
  }

  public void queueRescanProjectStructure() {
    if (LightEdit.owns(myProject)) return;
    LOG.debug("Queuing project rescan");
    myProjectRescanQueue.queue(Update.create("puppet.rescan", this::scanForEntities));
  }

  /**
   * Scanning project for puppet entities in the background thread
   */
  private void scanForEntities() {
    ReadAction.nonBlocking(() -> doScanForEntities())
      .inSmartMode(myProject)
      .coalesceBy(this)
      .expireWith(this)
      .submit(myUpdateExecutor);
  }

  @TestOnly
  public void waitForScan() {
    while (!myProjectRescanQueue.isEmpty()) {
      UIUtil.dispatchAllInvocationEvents();
      TimeoutUtil.sleep(10);
    }
    NonBlockingReadActionImpl.waitForAsyncTaskCompletion();
    while (!myUpdateExecutor.isEmpty()) {
      UIUtil.dispatchAllInvocationEvents();
      TimeoutUtil.sleep(10);
    }
    UIUtil.dispatchAllInvocationEvents();
  }

  private void doScanForEntities() {
    LOG.debug("Re-scanning project " + myProject);

    Set<VirtualFile> rootSet = new HashSet<>();
    Processor<VirtualFile> processor = file -> {
      ProgressManager.checkCanceled();
      VirtualFile root = file.getParent();
      if (root != null) {
        rootSet.add(root);
      }
      return true;
    };

    GlobalSearchScope scope = ProjectScope.getContentScope(myProject);

    FilenameIndex
      .getVirtualFilesByName(MODULE_META_FILE, scope)
      .forEach(processor::process);

    FilenameIndex
      .getVirtualFilesByName(ENVIRONMENT_META_FILE, scope)
      .forEach(processor::process);

    FilenameIndex
      .getVirtualFilesByName(PUPPET_FILE, scope)
      .forEach(processor::process);

    getModel().clear();

    rootSet.forEach(virtualFile -> {
      ProgressManager.checkCanceled();
      refreshModelForRoot(virtualFile);
    });
  }

  /**
   * Methods scans for implicit module roots under the root specified
   */
  private void scanForImplicitModules(@NotNull PuppetEntity<?> explicitEntity) {
    ReadAction.runBlocking(() -> {
      LOG.debug("Scanning for implicit modules for " + explicitEntity);
      if (!explicitEntity.isValid()) {
        LOG.debug("Skipping invalid entity: " + explicitEntity);
        return;
      }

      explicitEntity.getAllDependenciesRoots().stream()
        .flatMap(file -> Arrays.stream(file.getChildren()))
        .filter(file -> file.isValid() && file.isDirectory() && getModel().getPuppetModuleOrEnvironment(file) == null)
        .forEach(implicitRoot -> addImplicitModule(implicitRoot, explicitEntity.getRoot()
        ));
      LOG.debug("Done scanning for implicit modules for " + explicitEntity);
    });
  }

  /**
   * Re-scanning {@code root} for a known marker file: metadata.json, Puppetfile, etc. When file is found - new entity created for it
   * and added to the project model. If other entity existed in this root, it's going to be removed using {@link PuppetProjectModel#removeRoot(VirtualFile)}
   * If no keyfiles found in {@code root}, removes root from the model
   */
  private void refreshModelForRoot(@Nullable VirtualFile root) {
    ApplicationManager.getApplication().assertIsNonDispatchThread();
    ReadAction.run(() -> {
      if (root == null || myProject.isDisposed() || !root.isValid()) {
        return;
      }
      LOG.debug("Updating metadata for: " + root);

        VirtualFile keyFile = root.findChild(MODULE_META_FILE);
        PuppetEntity<?> newEntity = null;
        if (keyFile != null) {
          LOG.debug("Found a " + keyFile + ", reading metadata");
          PuppetModuleMetadata moduleMetadata = PuppetModuleMetadata.readMetadata(keyFile);
          if (moduleMetadata != null) {
            LOG.debug("Got metadata, creating a module");
            newEntity = new PuppetModule(myProject, root, moduleMetadata);
          }
          else {
            LOG.debug("No metadata found");
          }
        }
        else if (root.findChild(ENVIRONMENT_META_FILE) != null || root.findChild(PUPPET_FILE) != null) {
          LOG.debug("Found an environment.conf or a Puppetfile, creating an environment");
          newEntity = new PuppetEnvironment(myProject, root);
        }

        if (newEntity != null) {
          LOG.debug("Adding new entity: " + newEntity);
          getModel().removeRoot(root);
          getModel().addEntity(newEntity);
          scanForImplicitModules(newEntity);
        }
        else {
          PuppetEntity<?> existingEntity = getModel().getPuppetModuleOrEnvironment(root);
          if (existingEntity != null) {
            LOG.debug("Currently root assigned to entity: " + existingEntity);
          }
          if (existingEntity instanceof PuppetEnvironment ||
              existingEntity instanceof PuppetModule && !((PuppetModule)existingEntity).isHeadless()) {
            getModel().removeRoot(root);
          }
        }

      LOG.debug("Done updating metadata for " + root);
    });
  }

  public PuppetProjectModel getModel() {
    return myModel;
  }

  private void onProjectUpdated() {
    myUpdateQueue.queue(Update.create("puppet.update", this::notifyProjectUpdated));
  }

  private void notifyProjectUpdated() {
    if (myProject.isDisposed()) {
      return;
    }
    LOG.debug("Broadcasting project updated");
    myProject.getMessageBus().syncPublisher(PUPPET_PROJECT_TOPIC).projectUpdated();
    if (!ApplicationManager.getApplication().isUnitTestMode()) {
      ProjectView.getInstance(myProject).refresh(ProjectViewUpdateCause.PLUGIN_PUPPET);
    }
  }

  private void addImplicitModule(@NotNull VirtualFile implicitRoot, @NotNull VirtualFile parentRoot) {
    getModel().addImplicitEntity(
      new PuppetModule(myProject, implicitRoot, new PuppetHeadlessModuleMetadata(implicitRoot.getName())), parentRoot);
  }

  void clearMetaData(@NotNull VirtualFile entityRoot) {
    LOG.debug("Clearing metadata for " + entityRoot);
    getModel().removeRoot(entityRoot);
  }


  /**
   * Looks for puppet entity, containing passed file. If found entity is headless module, we are looking for it's container.
   */
  public @Nullable PuppetEntity<?> findExplicitModuleOrEnvironmentForFile(@Nullable VirtualFile file) {
    while (file != null) {
      PuppetEntity<?> entity = findModuleOrEnvironmentForFile(file);
      if (entity instanceof PuppetModule && ((PuppetModule)entity).isHeadless()) {
        file = entity.getRoot().getParent();
      }
      else {
        return entity;
      }
    }
    return null;
  }

  /**
   * @return nearest puppet entity, containing passed file.
   */
  public @Nullable PuppetEntity<?> findModuleOrEnvironmentForFile(@NotNull VirtualFile file) {
    PuppetDirectoryInfo puppetDirectoryInfo = PuppetDirectoryIndex.getInstance(myProject).getInfoForFile(file);
    if (!puppetDirectoryInfo.isAvailable()) {
      return null;
    }
    return getModel().getPuppetModuleOrEnvironment(puppetDirectoryInfo.getPuppetRoot());
  }

  @RequiresReadLock
  public @NotNull List<PuppetModule> getModulesInRoot(@Nullable VirtualFile root) {
    if (root == null || !root.isValid()) {
      return Collections.emptyList();
    }

    List<PuppetModule> result = new ArrayList<>();

    for (PuppetModule module : getModel().getAllModules()) {
      VirtualFile moduleRoot = module.getRoot();
      if (!moduleRoot.isValid()) {
        continue;
      }
      if (root.equals(moduleRoot.getParent())) {
        result.add(module);
      }
    }

    return result;
  }

  public Collection<PuppetModule> findModules(@NotNull String name) {
    return ContainerUtil.filter(getModel().getAllModules(), module -> (name.equals(module.getName()) || name.equals(module.getShortName())));
  }

  void scheduleScanRoot(@NotNull VirtualFile root) {
    myUpdateExecutor.execute(() -> refreshModelForRoot(root));
  }
}
