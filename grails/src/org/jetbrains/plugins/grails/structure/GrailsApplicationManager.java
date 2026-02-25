// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.structure;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.util.ModificationTracker;
import com.intellij.openapi.util.SimpleModificationTracker;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiFileSystemItem;
import com.intellij.psi.search.FilenameIndex;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.util.concurrency.AppExecutorUtil;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Holds {@link GrailsApplication Grails applications} in current {@link Project}
 */
@Service(Service.Level.PROJECT)
public final class GrailsApplicationManager implements ModificationTracker, Disposable {
  private static final ExecutorService ourExecutorService = AppExecutorUtil.createBoundedApplicationPoolExecutor("GrailsExecutor Pool", 1);
  private static final Logger LOG = Logger.getInstance(GrailsApplicationManager.class);

  private final SimpleModificationTracker myModificationTracker = new SimpleModificationTracker();
  private final @NotNull Project myProject;
  private final @NotNull ProjectFileIndex myFileIndex;
  private final AtomicReference<Map<VirtualFile, GrailsApplication>> myApplicationsRef = new AtomicReference<>(Collections.emptyMap());

  public GrailsApplicationManager(@NotNull Project project) {
    myProject = project;
    myFileIndex = ProjectFileIndex.getInstance(project);
    GrailsApplicationProvider.APPLICATION_PROVIDER.addChangeListener(() -> {
      if (project.isDisposed()) {
        return;
      }
      afterUpdate(myApplicationsRef.getAndSet(Collections.emptyMap())); // forget computed apps
      queueUpdate();
    }, this);
  }

  @Override
  public void dispose() {
     //no op
  }

  @Override
  public long getModificationCount() {
    return myModificationTracker.getModificationCount();
  }

  public boolean hasApplications() {
    return !getApplicationMap().isEmpty();
  }

  public @NotNull @Unmodifiable Collection<GrailsApplication> getApplications() {
    return ContainerUtil.filter(getApplicationMap().values(), GrailsApplication::isValid);
  }

  private @NotNull Map<VirtualFile, GrailsApplication> getApplicationMap() {
    return myApplicationsRef.get();
  }

  @Contract("null -> null")
  public @Nullable GrailsApplication findApplication(@Nullable VirtualFile file) {
    if (file == null || !hasApplications()) return null;
    for (VirtualFile currentFile = myFileIndex.getContentRootForFile(file); currentFile != null; currentFile = currentFile.getParent()) {
      final GrailsApplication application = getApplicationByRoot(currentFile);
      if (application != null) return application;
      if (currentFile.equals(myProject.getBaseDir())) break;
    }
    return null;
  }

  @Contract("null -> null")
  public @Nullable GrailsApplication getApplicationByRoot(@Nullable VirtualFile root) {
    if (root == null) return null;
    return getApplicationMap().get(root);
  }

  public @NotNull Future<?> queueUpdate() {
    if (LOG.isTraceEnabled()) {
      LOG.debug("Update queued from ", new Throwable());
    }
    else if (LOG.isDebugEnabled()) {
      LOG.debug("Update queued from " + new Throwable().getStackTrace()[2]);
    }

    return ReadAction.nonBlocking(this::doUpdate)
      .inSmartMode(myProject)
      .expireWith(this)
      .submit(ourExecutorService);
  }

  private void doUpdate() {
    Map<VirtualFile, GrailsApplication> oldMap = myApplicationsRef.get();
    Map<VirtualFile, GrailsApplication> map = doGetApplications();
    if (oldMap.equals(map)) return;

    if (myApplicationsRef.compareAndSet(oldMap, map)) {
      if (LOG.isDebugEnabled()) {
        LOG.debug("Grails applications recomputed");
        LOG.debug(map.values().toString());
      }
      afterUpdate(oldMap);
    }
    else {
      LOG.warn("Some thread already updated values");
    }
  }

  private void afterUpdate(@NotNull Map<VirtualFile, GrailsApplication> oldApplications) {
    oldApplications.values().forEach(GrailsApplication::invalidate);
    myModificationTracker.incModificationCount();
    myProject.getMessageBus().syncPublisher(GrailsApplicationListener.TOPIC).applicationsRecomputed();
  }

  private @NotNull Map<VirtualFile, GrailsApplication> doGetApplications() {
    ProgressManager.checkCanceled();
    final Collection<VirtualFile> appRoots = FilenameIndex
      .getVirtualFilesByName("grails-app", GlobalSearchScope.allScope(myProject));
    if (appRoots.isEmpty()) return Collections.emptyMap();

    final Map<VirtualFile, GrailsApplication> result = new HashMap<>();
    for (VirtualFile appRoot : appRoots) {
      if (!appRoot.isDirectory()) continue;
      ProgressManager.checkCanceled();
      final VirtualFile root = appRoot.getParent();
      if (root != null && root.isDirectory()) {
        final GrailsApplication application = GrailsApplicationProvider.createGrailsApplication(myProject, root);
        if (application != null) result.put(root, application);
      }
    }
    return result;
  }

  public static @NotNull GrailsApplicationManager getInstance(@NotNull Project project) {
    return project.getService(GrailsApplicationManager.class);
  }

  @Contract("null -> null")
  public static @Nullable GrailsApplication findApplication(@Nullable PsiElement element) {
    if (element == null) return null;

    final VirtualFile file;
    if (element instanceof PsiFile) {
      file = ((PsiFile)element).getOriginalFile().getVirtualFile();
    }
    else if (element instanceof PsiFileSystemItem) {
      file = ((PsiFileSystemItem)element).getVirtualFile();
    }
    else {
      final PsiFile psiFile = element.getContainingFile();
      file = psiFile == null ? null : psiFile.getOriginalFile().getVirtualFile();
    }

    return getInstance(element.getProject()).findApplication(file);
  }

  @Contract("null -> null")
  public static @Nullable OldGrailsApplication findApplication(@Nullable Module module) {
    if (module == null) return null;
    for (GrailsApplication application : getInstance(module.getProject()).getApplications()) {
      if (application instanceof OldGrailsApplication) {
        if (((OldGrailsApplication)application).getModule().equals(module)) {
          return (OldGrailsApplication)application;
        }
      }
    }
    return null;
  }
}
