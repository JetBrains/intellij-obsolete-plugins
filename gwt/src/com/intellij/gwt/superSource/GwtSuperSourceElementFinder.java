package com.intellij.gwt.superSource;

import com.intellij.concurrency.ConcurrentCollectionFactory;
import com.intellij.facet.ProjectFacetManager;
import com.intellij.gwt.facet.GwtFacetType;
import com.intellij.ide.highlighter.JavaFileType;
import com.intellij.openapi.fileTypes.FileTypeRegistry;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModuleRootEvent;
import com.intellij.openapi.roots.ModuleRootListener;
import com.intellij.openapi.util.LowMemoryWatcher;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.openapi.vfs.newvfs.BulkFileListener;
import com.intellij.openapi.vfs.newvfs.events.VFileEvent;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassOwner;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElementFinder;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiPackage;
import com.intellij.psi.PsiTreeChangeAdapter;
import com.intellij.psi.PsiTreeChangeEvent;
import com.intellij.psi.impl.file.PsiPackageImpl;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.util.Processor;
import com.intellij.util.SmartList;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.messages.MessageBusConnection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jps.gwt.index.GwtModuleXmlConstants;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static java.util.Collections.emptyMap;

public final class GwtSuperSourceElementFinder extends PsiElementFinder {
  private final Project myProject;
  private final PsiManager myPsiManager;
  private final Map<String, List<VirtualFile>> mySuperSourceDirectoriesByPackageName = new ConcurrentHashMap<>();
  private final Set<String> myNonExistentSuperSourcePackages = ConcurrentCollectionFactory.createConcurrentSet();

  // cache that maps directories to pairs <className, containingFile> for all classes in the directory;
  // list of pair is invalidated on any Java file change in the directory
  private final Map<VirtualFile, Map<String, VirtualFile>> mySuperSourceClassesByDirectory = new ConcurrentHashMap<>();

  public GwtSuperSourceElementFinder(Project project) {
    myProject = project;
    myPsiManager = PsiManager.getInstance(project);
    // Tie all listener registrations to the plugin-scoped cache service so they are removed when the plugin is
    // unloaded; a parent-less message-bus connection / PSI tree change listener would otherwise retain this finder
    // (and its PluginClassLoader) forever, leaking memory on dynamic plugin unload (IDEA-339371).
    GwtSuperSourceClassCacheImpl cacheService = (GwtSuperSourceClassCacheImpl)GwtSuperSourceClassCache.getInstance(project);
    MessageBusConnection busConnection = myProject.getMessageBus().connect(cacheService);
    busConnection.subscribe(ModuleRootListener.TOPIC, new ModuleRootListener() {
      @Override
      public void rootsChanged(@NotNull ModuleRootEvent event) {
        clearCaches();
      }
    });
    busConnection.subscribe(VirtualFileManager.VFS_CHANGES, new BulkFileListener() {

      @Override
      public void after(@NotNull List<? extends @NotNull VFileEvent> events) {
        for (VFileEvent event : events) {
          VirtualFile file = event.getFile();
          if (file != null && FileTypeRegistry.getInstance().getFileTypeByFileName(file.getName()).equals(JavaFileType.INSTANCE)) {
            clearDirectoryCache(file.getParent());
          }
        }
      }
    });
    myPsiManager.addPsiTreeChangeListener(new PsiTreeChangeAdapter() {
      private void processChange(PsiTreeChangeEvent event) {
        PsiFile file = event.getFile();
        if (file != null) {
          if (file.getName().endsWith(GwtModuleXmlConstants.GWT_XML_SUFFIX)) {
            clearCaches();
          }
          PsiDirectory psiDirectory = file.getContainingDirectory();
          if (psiDirectory != null) {
            clearDirectoryCache(psiDirectory.getVirtualFile());
          }
        }
      }

      @Override
      public void childAdded(@NotNull PsiTreeChangeEvent event) {
        processChange(event);
      }

      @Override
      public void childRemoved(@NotNull PsiTreeChangeEvent event) {
        processChange(event);
      }

      @Override
      public void childReplaced(@NotNull PsiTreeChangeEvent event) {
        processChange(event);
      }

      @Override
      public void childMoved(@NotNull PsiTreeChangeEvent event) {
        processChange(event);
      }

      @Override
      public void childrenChanged(@NotNull PsiTreeChangeEvent event) {
        processChange(event);
      }
    }, cacheService);

    LowMemoryWatcher.register(() -> {
      myNonExistentSuperSourcePackages.clear();
      mySuperSourceClassesByDirectory.clear();
    }, cacheService);
  }

  private void clearCaches() {
    myNonExistentSuperSourcePackages.clear();
    mySuperSourceDirectoriesByPackageName.clear();
    mySuperSourceClassesByDirectory.clear();
  }

  private void clearDirectoryCache(VirtualFile packageDirectory) {
    if (packageDirectory != null) {
      mySuperSourceClassesByDirectory.remove(packageDirectory);
    }
  }

  private List<VirtualFile> getDirectoriesByPackageName(String packageName) {
    List<VirtualFile> cached = mySuperSourceDirectoriesByPackageName.get(packageName);
    if (cached != null) return cached;

    if (myNonExistentSuperSourcePackages.contains(packageName)) return Collections.emptyList();

    List<VirtualFile> result = findDirectoriesByPackageName(packageName);
    if (result.isEmpty()) {
      myNonExistentSuperSourcePackages.add(packageName);
    }
    else {
      mySuperSourceDirectoriesByPackageName.put(packageName, result);
    }
    return result;
  }

  private @NotNull List<VirtualFile> findDirectoriesByPackageName(String packageName) {
    if (StringUtil.isEmpty(packageName)) {
      return GwtModuleSuperSourceIndex.getSuperSourceRoots(GlobalSearchScope.allScope(myProject));
    }

    List<VirtualFile> parentDirectories = getDirectoriesByPackageName(StringUtil.getPackageName(packageName));
    if (parentDirectories.isEmpty()) return Collections.emptyList();

    List<VirtualFile> result = new SmartList<>();
    String shortName = StringUtil.getShortName(packageName);
    for (VirtualFile parentDirectory : parentDirectories) {
      VirtualFile child = parentDirectory.findChild(shortName);
      if (child != null && child.isDirectory()) {
        result.add(child);
      }
    }
    return result;
  }

  @Override
  public @Nullable PsiPackage findPackage(@NotNull String qualifiedName) {
    if (!hasGwtFacets() || DumbService.isDumb(myProject)) return null;
    if (!getDirectoriesByPackageName(qualifiedName).isEmpty()) {
      return new PsiPackageImpl(myPsiManager, qualifiedName);
    }
    return null;
  }

  @Override
  public PsiClass findClass(@NotNull String qualifiedName, @NotNull GlobalSearchScope scope) {
    return ContainerUtil.getFirstItem(doFindClasses(qualifiedName, scope));
  }

  @Override
  public PsiClass @NotNull [] findClasses(@NotNull String qualifiedName, @NotNull GlobalSearchScope scope) {
    return ContainerUtil.toArray(doFindClasses(qualifiedName, scope), PsiClass.ARRAY_FACTORY);
  }

  private @NotNull List<PsiClass> doFindClasses(@NotNull String qualifiedName, @NotNull GlobalSearchScope scope) {
    if (!hasGwtFacets()) return Collections.emptyList();

    List<PsiClass> result = new SmartList<>();
    List<String> innerClassNames = new SmartList<>();
    GwtSuperSourceClassCache classCache = GwtSuperSourceClassCache.getInstance(myProject);
    String currentName = qualifiedName;
    do {
      String packageName = StringUtil.getPackageName(currentName);
      String className = StringUtil.getShortName(currentName);
      //optimization: otherwise we will process children of all root packages
      if (!className.isEmpty() && Character.isLowerCase(className.charAt(0)) && !innerClassNames.isEmpty()) break;

      ITERATE_DIRECTORIES:
      for (VirtualFile directory : getDirectoriesByPackageName(packageName)) {
        if (directory.isValid() && scope.contains(directory)) {
          Map<String, VirtualFile> cachedMap = mySuperSourceClassesByDirectory.computeIfAbsent(directory, d -> {
            Map<String, VirtualFile> classNameToFile = new HashMap<>();
            for (VirtualFile file : directory.getChildren()) {
              final PsiFile psiFile = classCache.getCachedPsiFile(file);
              if (psiFile instanceof PsiJavaFile) {
                for (PsiClass aClass : ((PsiJavaFile)psiFile).getClasses()) {
                  classNameToFile.put(aClass.getQualifiedName(), file);
                }
              }
            }

            // these maps are read-only, so packing them this way reduces memory usage
            return classNameToFile.isEmpty() ? emptyMap() : new HashMap<>(classNameToFile);
          });
          VirtualFile file = cachedMap.get(currentName);
          if (file == null || !scope.contains(file)) {
            continue ITERATE_DIRECTORIES;
          }

          PsiFile psiFile = classCache.getCachedPsiFile(file);
          if (psiFile instanceof PsiJavaFile) {
            for (PsiClass aClass : ((PsiJavaFile)psiFile).getClasses()) {
              if (currentName.equals(aClass.getQualifiedName())) {
                ContainerUtil.addIfNotNull(result, GwtSuperSourceClassCache.findInnerClass(aClass, innerClassNames));
                continue ITERATE_DIRECTORIES;
              }
            }
          }
        }
      }
      innerClassNames.add(className);
      currentName = packageName;
    }
    while (!currentName.isEmpty());
    return result;
  }

  private boolean hasGwtFacets() {
    return ProjectFacetManager.getInstance(myProject).hasFacets(GwtFacetType.ID);
  }

  @Override
  public boolean processPackageDirectories(final @NotNull PsiPackage psiPackage,
                                           @NotNull GlobalSearchScope scope,
                                           final @NotNull Processor<? super PsiDirectory> consumer,
                                           boolean includeLibrarySources) {
    if (DumbService.isDumb(myProject) || !hasGwtFacets()) return true;

    for (VirtualFile directory : getDirectoriesByPackageName(psiPackage.getQualifiedName())) {
      if (scope.contains(directory)) {
        final PsiDirectory psiDirectory = myPsiManager.findDirectory(directory);
        if (psiDirectory != null && !consumer.process(psiDirectory)) {
          return false;
        }
      }
    }
    return true;
  }

  @Override
  public PsiClass @NotNull [] getClasses(@NotNull PsiPackage psiPackage, @NotNull GlobalSearchScope scope) {
    if (DumbService.isDumb(myProject) || !hasGwtFacets()) return PsiClass.EMPTY_ARRAY;

    List<PsiClass> classes = null;
    for (VirtualFile packageDir : getDirectoriesByPackageName(psiPackage.getQualifiedName())) {
      if (scope.contains(packageDir)) {
        for (VirtualFile file : packageDir.getChildren()) {
          if (scope.contains(file) && FileTypeRegistry.getInstance().isFileOfType(file, JavaFileType.INSTANCE)) {
            PsiFile psiFile = GwtSuperSourceClassCache.getInstance(myProject).getCachedPsiFile(file);
            if (psiFile instanceof PsiClassOwner) {
              if (classes == null) {
                classes = new ArrayList<>();
              }
              Collections.addAll(classes, ((PsiClassOwner)psiFile).getClasses());
            }
          }
        }
      }
    }
    return classes != null ? classes.toArray(PsiClass.EMPTY_ARRAY) : PsiClass.EMPTY_ARRAY;
  }
}
