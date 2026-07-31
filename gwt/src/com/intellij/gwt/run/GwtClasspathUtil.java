package com.intellij.gwt.run;

import com.intellij.gwt.facet.GwtFacet;
import com.intellij.gwt.module.GwtModulesManager;
import com.intellij.gwt.module.model.GwtModule;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.LibraryOrderEntry;
import com.intellij.openapi.roots.ModuleOrderEntry;
import com.intellij.openapi.roots.ModuleSourceOrderEntry;
import com.intellij.openapi.roots.OrderEntry;
import com.intellij.openapi.roots.OrderEnumerator;
import com.intellij.openapi.roots.OrderRootType;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.openapi.util.Condition;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.PathsList;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Set;

public final class GwtClasspathUtil {
  private GwtClasspathUtil() {
  }

  private static Condition<OrderEntry> createGwtRelatedEntryCondition(final Project project) {
    final Set<VirtualFile> gwtModulesSourceRoots = new HashSet<>();
    final ProjectFileIndex index = ProjectRootManager.getInstance(project).getFileIndex();
    for (GwtModule gwtModule : GwtModulesManager.getInstance(project).getAllGwtModules()) {
      final VirtualFile sourceRoot = index.getSourceRootForFile(gwtModule.getModuleFile());
      ContainerUtil.addIfNotNull(gwtModulesSourceRoots, sourceRoot);
    }

    return orderEntry -> {
      if (orderEntry instanceof ModuleOrderEntry) return true;
      if (orderEntry instanceof ModuleSourceOrderEntry) {
        return GwtFacet.getInstance(((ModuleSourceOrderEntry)orderEntry).getRootModel().getModule()) != null;
      }
      if (orderEntry instanceof LibraryOrderEntry) {
        final Library library = ((LibraryOrderEntry)orderEntry).getLibrary();
        if (library != null) {
          Set<VirtualFile> classesRoots = ContainerUtil.newHashSet(library.getFiles(OrderRootType.CLASSES));
          for (VirtualFile sourceRoot : library.getFiles(OrderRootType.SOURCES)) {
            if (!classesRoots.contains(sourceRoot) && gwtModulesSourceRoots.contains(sourceRoot)) {
              return true;
            }
          }
        }
        return false;
      }
      return false;
    };
  }

  public static PathsList getSourceRootsOfGwtModules(final @NotNull Module module, final boolean productionOnly) {
    return enumerateEntriesWithGwtSourceRoots(module, productionOnly).getSourcePathsList();
  }

  public static OrderEnumerator enumerateEntriesWithGwtSourceRoots(Module module, boolean productionOnly) {
    OrderEnumerator enumerator = OrderEnumerator.orderEntries(module);
    if (productionOnly) {
      enumerator = enumerator.productionOnly();
    }
    return enumerator.withoutSdk().satisfying(createGwtRelatedEntryCondition(module.getProject())).recursively();
  }
}
