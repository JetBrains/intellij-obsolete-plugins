package com.intellij.lang.puppet.project;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.containers.MultiMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.TestOnly;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class PuppetProjectModel {
  private static final Logger LOG = Logger.getInstance(PuppetProjectModel.class);
  private final ReentrantReadWriteLock myLock = new ReentrantReadWriteLock(true);
  private final Map<VirtualFile, PuppetEntity> myRootsMap = new HashMap<>();
  private final MultiMap<VirtualFile, VirtualFile> myImplicitRootsMap = new MultiMap<>();
  private Runnable myChangeListener;

  /**
   * Clears the model
   */
  void clear() {
    if (write(() -> {
      int rootsNumber = myRootsMap.size() + myImplicitRootsMap.size();
      myRootsMap.clear();
      myImplicitRootsMap.clear();
      return rootsNumber;
    }) > 0) {
      onChange();
    }
  }

  public @NotNull List<VirtualFile> getAllRoots() {
    return read(() -> new ArrayList<>(myRootsMap.keySet()));
  }

  public @NotNull List<PuppetModule> getAllModules() {
    return read(
      () -> ContainerUtil.mapNotNull(myRootsMap.values(), entity -> entity instanceof PuppetModule ? (PuppetModule)entity : null));
  }

  @TestOnly
  public Pair<Map<VirtualFile, PuppetEntity>, MultiMap<VirtualFile, VirtualFile>> getRootsData() {
    return read(() -> Pair.create(new HashMap<>(myRootsMap), new MultiMap<>(myImplicitRootsMap)));
  }

  /**
   * Adds an {@code entity} to the project model.
   */
  void addEntity(@NotNull PuppetEntity entity) {
    LOG.debug("Adding entity " + entity);
    if (write(() -> {
      if (entity.equals(myRootsMap.get(entity.getRoot()))) {
        return false;
      }
      myRootsMap.put(entity.getRoot(), entity);
      return true;
    })) {
      onChange();
    }
  }

  /**
   * Removing {@code root} from the project model with entity and related implicit modules
   */
  void removeRoot(@NotNull VirtualFile root) {
    LOG.debug("Removing root: " + root);
    if (write(() -> {
      if (!myRootsMap.containsKey(root)) {
        return false;
      }
      myRootsMap.remove(root);
      for (Iterator<VirtualFile> iterator = myImplicitRootsMap.get(root).iterator(); iterator.hasNext(); ) {
        myRootsMap.remove(iterator.next());
        iterator.remove();
      }
      return true;
    })) {
      onChange();
    }
  }

  private void onChange() {
    Runnable listener = myChangeListener;
    if (listener != null) {
      listener.run();
    }
  }

  void addImplicitEntity(@NotNull PuppetEntity entity, @NotNull VirtualFile parentEntityRoot) {
    LOG.debug("Adding implicit entity " + entity + " for parent entityRoot " + parentEntityRoot);
    write(() -> {
      myImplicitRootsMap.putValue(parentEntityRoot, entity.getRoot());
      addEntity(entity);
    });
  }

  public @Nullable PuppetEntity getPuppetModuleOrEnvironment(@Nullable VirtualFile root) {
    if (root == null) {
      return null;
    }
    return read(() -> myRootsMap.get(root));
  }

  public void setChangeListener(@Nullable Runnable changeListener) {
    myChangeListener = changeListener;
  }

  /**
   * Computes a {@code computable} with guaranteed consistent puppet project model, must not perform any changes to the model
   */
  private <T> T read(@NotNull Computable<T> computable) {
    myLock.readLock().lock();
    try {
      return computable.compute();
    }
    finally {
      myLock.readLock().unlock();
    }
  }

  /**
   * @see #write(Computable)
   */
  private void write(@NotNull Runnable runnable) {
    write(() -> {
      runnable.run();
      return null;
    });
  }

  /**
   * Computes a {@code computable} with write permission, allowed alter project model if necessary
   */
  private <T> T write(@NotNull Computable<T> computable) {
    myLock.writeLock().lock();
    try {
      return computable.compute();
    }
    finally {
      myLock.writeLock().unlock();
    }
  }

  public static @NotNull PuppetProjectModel getInstance(@NotNull Project project) {
    return PuppetProjectManager.getInstance(project).getModel();
  }
}
