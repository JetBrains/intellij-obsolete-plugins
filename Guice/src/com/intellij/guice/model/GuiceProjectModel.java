// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.guice.model;

import com.intellij.codeInsight.AnnotationUtil;
import com.intellij.guice.GuiceBundle;
import com.intellij.guice.constants.GuiceAnnotations;
import com.intellij.guice.model.beans.BindDescriptor;
import com.intellij.guice.model.jam.GuiceProvides;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassOwner;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiParameter;
import com.intellij.psi.impl.compiled.ClsFileImpl;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.searches.AnnotatedElementsSearch;
import com.intellij.psi.util.CachedValueProvider;
import com.intellij.psi.util.CachedValuesManager;
import com.intellij.psi.util.InheritanceUtil;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Project-level service that owns a single {@link GuiceLiveIndex} and keeps it up-to-date
 * via surgical per-file updates.
 *
 * <h3>Design — truly incremental updates</h3>
 * <ul>
 *   <li><b>Single live index</b>: A {@link GuiceLiveIndex} is created once and never rebuilt
 *       from scratch (unless project structure changes).  Each file's Guice data (bindings,
 *       injection points, {@code @Provides}) is extracted independently and stored per-file
 *       inside the live index.</li>
 *   <li><b>Lazy dirty processing</b>: When a file changes (notified by {@link GuiceVfsListener}),
 *       it is added to a dirty set.  On the next {@link #getIndex(Module)} call, only the
 *       dirty files are re-processed — not the entire module.  This means editing one file
 *       re-extracts only that file's Guice data, leaving all other files untouched.</li>
 *   <li><b>Initial population</b>: On first access (or after a structure change), all relevant
 *       files are discovered using {@link AnnotatedElementsSearch} (for {@code @Inject} and
 *       {@code @Provides} annotations) and {@link GuiceInjectorManager#getGuiceModuleClasses}
 *       (for Guice module classes containing bindings).  Each discovered file is processed
 *       exactly once.</li>
 *   <li><b>Modification stamps</b>: Per-file VFS modification stamps are tracked to skip
 *       spurious VFS events where the file content hasn't actually changed.</li>
 *   <li><b>Thread safety</b>: {@link #getIndex(Module)} is called from highlighting threads.
 *       The dirty set uses {@link ConcurrentHashMap#newKeySet()}.  The {@link GuiceLiveIndex}
 *       itself is thread-safe for concurrent reads and single-writer updates.</li>
 * </ul>
 *
 * @see GuiceLiveIndex
 * @see GuiceVfsListener
 */
@Service(Service.Level.PROJECT)
public final class GuiceProjectModel implements Disposable {

  private final Project myProject;

  /**
   * The live index — never rebuilt from scratch (except on structure change),
   * only surgically updated per-file.
   */
  private final GuiceLiveIndex myLiveIndex = new GuiceLiveIndex();

  /**
   * Files marked dirty by {@link GuiceVfsListener}.  Processed lazily on the next
   * {@link #getIndex(Module)} call.  Uses a concurrent set for thread safety since
   * VFS events may fire on any thread.
   */
  private final Set<VirtualFile> myDirtyFiles = ConcurrentHashMap.newKeySet();

  /**
   * Set to {@code true} when the project structure changes (modules or libraries
   * added/removed, or a file is deleted).  This triggers a full re-population on
   * the next {@link #getIndex(Module)} call.
   */
  private volatile boolean myStructureChanged = true;

  /**
   * Whether the initial population has been performed.  Set to {@code true} after
   * {@link #performInitialPopulation(Module)} completes successfully.
   */
  private volatile boolean myInitialized = false;

  /**
   * Per-file VFS modification stamps.  Used to detect whether a file in the dirty
   * set has actually changed (VFS may fire spurious events).
   */
  private final ConcurrentHashMap<VirtualFile, Long> myFileStamps = new ConcurrentHashMap<>();

  // -----------------------------------------------------------------------
  // Construction / service access
  // -----------------------------------------------------------------------

  public GuiceProjectModel(@NotNull Project project) {
    myProject = project;
  }

  /**
   * Returns the singleton instance of this service for the given project.
   *
   * @param project the current project
   * @return the project-level {@link GuiceProjectModel} instance
   */
  public static @NotNull GuiceProjectModel getInstance(@NotNull Project project) {
    return project.getService(GuiceProjectModel.class);
  }

  // -----------------------------------------------------------------------
  // Main query API
  // -----------------------------------------------------------------------

  /**
   * Returns the current {@link GuiceLiveIndex} for the given IntelliJ module.
   *
   * <p>Ensures the index is up-to-date by:
   * <ol>
   *   <li>Performing initial population if this is the first call or a structure change
   *       has been detected.</li>
   *   <li>Processing any files that have been marked dirty since the last call.</li>
   * </ol>
   *
   * <p>This method is called from highlighting threads (background) and is thread-safe.
   *
   * @param module the IntelliJ module whose Guice index is requested
   * @return the current live index, never {@code null}
   */
  public @NotNull GuiceLiveIndex getIndex(@NotNull Module module) {
    if (!myInitialized || myStructureChanged) {
      synchronized (this) {
        // Double-check after acquiring lock to avoid duplicate population.
        if (!myInitialized || myStructureChanged) {
          performInitialPopulation(module);
        }
      }
    }
    processDirtyFiles();
    return myLiveIndex;
  }

  /**
   * Quick check: does this module have Guice on its classpath?
   * Checks if {@code com.google.inject.Inject} is resolvable.
   * Result is cached per-module and invalidated when project roots change.
   *
   * @param module the module to check
   * @return {@code true} if Guice is available on this module's classpath
   */
  public boolean isGuiceAvailable(@NotNull Module module) {
    return CachedValuesManager.getManager(myProject).getCachedValue(module, () -> {
      GlobalSearchScope scope = GlobalSearchScope.moduleWithDependenciesAndLibrariesScope(module);
      boolean available = JavaPsiFacade.getInstance(myProject)
          .findClass(GuiceAnnotations.INJECT, scope) != null;
      // Use ProjectRootManager to avoid invalidation on non-Java source changes.
      return CachedValueProvider.Result.create(available, ProjectRootManager.getInstance(myProject));
    });
  }

  // -----------------------------------------------------------------------
  // Event API — called by GuiceVfsListener
  // -----------------------------------------------------------------------

  /**
   * Marks a file as needing re-extraction of its Guice data.
   *
   * <p>The file is added to the dirty set and will be processed lazily on the
   * next {@link #getIndex(Module)} call.  Only the changed file's data will be
   * re-extracted and surgically updated in the live index.
   *
   * @param file the file that has changed
   */
  void markFileDirty(@NotNull VirtualFile file) {
    myDirtyFiles.add(file);
  }

  /**
   * Removes a file's data entirely (the file was deleted).
   *
   * <p>Immediately removes the file's contributions from the live index and
   * clears its stamp tracking.  No full re-population is triggered — the
   * surgical {@link GuiceLiveIndex#removeFile(VirtualFile)} is sufficient.
   *
   * @param file the file that was deleted
   */
  void removeFile(@NotNull VirtualFile file) {
    myLiveIndex.removeFile(file);
    myFileStamps.remove(file);
    myDirtyFiles.remove(file);
  }

  // -----------------------------------------------------------------------
  // Disposable
  // -----------------------------------------------------------------------

  @Override
  public void dispose() {
    myLiveIndex.clear();
    myFileStamps.clear();
    myDirtyFiles.clear();
    myInitialized = false;
  }

  // -----------------------------------------------------------------------
  // Internal: initial population
  // -----------------------------------------------------------------------

  /**
   * Performs a complete (re-)population of the live index.
   *
   * <p>Called on first access or after a structure change.  Clears all existing data
   * and discovers all relevant files using:
   * <ul>
   *   <li>{@link AnnotatedElementsSearch} for {@code @Inject} fields/methods and
   *       {@code @Provides} methods.</li>
   *   <li>{@link GuiceInjectorManager#getGuiceModuleClasses(Module, GlobalSearchScope)}
   *       for Guice module classes (which contain bindings in {@code configure()}).</li>
   * </ul>
   *
   * <p>Each discovered file is processed exactly once via {@link #processFile(VirtualFile)}.
   *
   * @param module the IntelliJ module whose scope defines what files to include
   */
  private void performInitialPopulation(@NotNull Module module) {
    myLiveIndex.clear();
    myFileStamps.clear();
    myDirtyFiles.clear();

    Set<VirtualFile> relevantFiles = discoverRelevantFiles(module);
    if (relevantFiles.isEmpty()) {
      myStructureChanged = false;
      myInitialized = true;
      return;
    }

    processFilesWithProgress(
        GuiceBundle.message("progress.building.guice.model"),
        relevantFiles
    );

    myStructureChanged = false;
    myInitialized = true;
  }

  /**
   * Discovers all files in the module's scope that contain Guice-relevant annotations
   * or Guice module classes.
   *
   * @param module the IntelliJ module whose scope defines what files to include
   * @return a set of virtual files that should be processed for Guice data
   */
  private @NotNull Set<VirtualFile> discoverRelevantFiles(@NotNull Module module) {
    Set<VirtualFile> files = new HashSet<>();
    GlobalSearchScope scope = GlobalSearchScope.moduleWithDependenciesAndLibrariesScope(module);
    JavaPsiFacade facade = JavaPsiFacade.getInstance(myProject);

    // Files with @Inject fields/methods
    for (String injectAnno : GuiceAnnotations.INJECTS) {
      PsiClass annoClass = facade.findClass(injectAnno, GlobalSearchScope.allScope(myProject));
      if (annoClass == null) continue;
      for (PsiField field : AnnotatedElementsSearch.searchPsiFields(annoClass, scope).findAll()) {
        addFileOf(field, files);
      }
      for (PsiMethod method : AnnotatedElementsSearch.searchPsiMethods(annoClass, scope).findAll()) {
        addFileOf(method, files);
      }
    }

    // Files with @Provides methods
    for (String providesAnno : GuiceAnnotations.ALL_PROVIDES_ANNOTATIONS) {
      PsiClass annoClass = facade.findClass(providesAnno, GlobalSearchScope.allScope(myProject));
      if (annoClass == null) continue;
      for (PsiMethod method : AnnotatedElementsSearch.searchPsiMethods(annoClass, scope).findAll()) {
        addFileOf(method, files);
      }
    }

    // Guice module files (for bindings defined in configure())
    GlobalSearchScope bindingScope = GlobalSearchScope.moduleWithDependenciesAndLibrariesScope(module);
    for (PsiClass cls : GuiceInjectorManager.getGuiceModuleClasses(module, bindingScope)) {
      addFileOf(cls, files);
    }

    return files;
  }

  /**
   * Adds the containing virtual file of a PSI element to the file set.
   *
   * <p>For elements from compiled {@code .class} files, attempts to resolve the
   * corresponding source file via {@link PsiElement#getNavigationElement()}.  If a
   * source file is available (e.g., from an attached source JAR), the <em>source</em>
   * VirtualFile is added instead — this ensures {@link #processFile(VirtualFile)}
   * receives a file with full method bodies, enabling binding extraction.
   *
   * <p>If no source is available, the {@code .class} VirtualFile is added as-is;
   * {@link #processFile(VirtualFile)} will still extract {@code @Inject} IPs and
   * {@code @Provides} from bytecode annotations, but skip bindings.
   *
   * @param element the PSI element whose file should be added
   * @param files   the set to add the file to
   */
  private static void addFileOf(@NotNull PsiElement element, @NotNull Set<VirtualFile> files) {
    PsiFile file = element.getContainingFile();
    if (file == null) return;

    // For compiled classes, prefer the source file if available.
    if (file instanceof ClsFileImpl clsFile) {
      PsiElement sourceNav = clsFile.getNavigationElement();
      if (sourceNav instanceof PsiFile sourceFile && sourceFile != clsFile) {
        VirtualFile sourceVf = sourceFile.getVirtualFile();
        if (sourceVf != null) {
          files.add(sourceVf);
          return;
        }
      }
    }

    VirtualFile vf = file.getVirtualFile();
    if (vf != null) files.add(vf);
  }

  // -----------------------------------------------------------------------
  // Internal: per-file processing
  // -----------------------------------------------------------------------

  /**
   * Extracts all Guice data (bindings, injection points, {@code @Provides} methods)
   * from a single file and updates the live index surgically.
   *
   * <h4>Compiled class file handling</h4>
   * <p>When {@code vf} points to a compiled {@code .class} file (e.g., from a library JAR),
   * this method attempts to resolve the corresponding source file via
   * {@link ClsFileImpl#getNavigationElement()}.  If a source file is available:
   * <ul>
   *   <li>The source file is used for all extraction (bindings, IPs, {@code @Provides}).
   *       This gives us full method bodies, enabling binding extraction from
   *       {@code configure()} and {@code getProvider()} call detection.</li>
   *   <li>The index is keyed by the <em>original</em> {@code .class} VirtualFile (since
   *       that's what the VFS listener sees for library files).</li>
   * </ul>
   * <p>If no source is available (e.g., hjars without source JARs):
   * <ul>
   *   <li>{@code @Inject} injection points and {@code @Provides} methods are still extracted
   *       from bytecode annotations (which are preserved in {@code .class} files).</li>
   *   <li>Bindings are <em>not</em> extracted (they require method body traversal).</li>
   *   <li>Binding-call IPs ({@code getProvider()}, {@code .to()}) are <em>not</em>
   *       extracted (they require method body search).</li>
   * </ul>
   *
   * @param vf the virtual file to process
   */
  private void processFile(@NotNull VirtualFile vf) {
    PsiFile psiFile = PsiManager.getInstance(myProject).findFile(vf);
    if (psiFile == null) {
      myLiveIndex.removeFile(vf);
      myFileStamps.remove(vf);
      return;
    }

    // For compiled class files, try to resolve to source for full extraction.
    PsiFile fileForExtraction = psiFile;
    boolean isCompiled = psiFile instanceof ClsFileImpl;
    if (isCompiled) {
      PsiElement sourceNav = ((ClsFileImpl) psiFile).getNavigationElement();
      if (sourceNav instanceof PsiFile sourceFile && sourceFile != psiFile) {
        fileForExtraction = sourceFile;
        isCompiled = false; // We have source — treat as a regular source file.
      }
    }

    // Extract bindings (only from source files that contain Guice module classes).
    // Compiled files without source cannot provide method bodies for binding extraction.
    Set<BindDescriptor> bindings = Set.of();
    if (!isCompiled && !GuiceInjectorManager.collectGuiceModuleClasses(fileForExtraction).isEmpty()) {
      bindings = GuiceInjectorManager.getBindingsInFile(fileForExtraction);
    }

    // Extract injection points and @Provides.
    // For compiled files without source, AnnotationUtil.isAnnotated() still works
    // (annotations are preserved in bytecode), so @Inject IPs and @Provides are
    // correctly extracted.  Only binding-call IPs (getProvider(), .to()) are missed
    // since those require method body search — acceptable since library code is
    // rarely edited and these are Guice-internal patterns.
    Set<InjectionPointDescriptor> ips = new HashSet<>();
    List<GuiceProvides> provides = new ArrayList<>();
    extractGuiceElements(fileForExtraction, ips, provides);

    // Key the index by the original VirtualFile (the one the VFS listener tracks).
    myLiveIndex.updateFile(vf, bindings, ips, provides);
    myFileStamps.put(vf, vf.getModificationStamp());
  }

  /**
   * Processes all files in the dirty set.  For each dirty file, re-extracts its
   * Guice data and surgically updates the live index.  Invalid files (deleted
   * between marking dirty and processing) are removed from the index.
   *
   * <p>Modification stamps are checked to skip files that were marked dirty but
   * haven't actually changed (VFS may fire spurious events).
   */
  private void processDirtyFiles() {
    if (myDirtyFiles.isEmpty()) return;

    // Snapshot and clear the dirty set atomically to avoid processing the same
    // file multiple times.
    Set<VirtualFile> dirty = new HashSet<>(myDirtyFiles);
    myDirtyFiles.removeAll(dirty);

    // Filter out files with unchanged stamps (VFS may fire spurious events)
    // and invalid files (deleted between marking dirty and processing).
    Set<VirtualFile> toProcess = new LinkedHashSet<>();
    for (VirtualFile file : dirty) {
      if (!file.isValid()) {
        myLiveIndex.removeFile(file);
        myFileStamps.remove(file);
        continue;
      }
      Long oldStamp = myFileStamps.get(file);
      long currentStamp = file.getModificationStamp();
      if (oldStamp != null && oldStamp == currentStamp) {
        continue; // No actual change
      }
      toProcess.add(file);
    }

    if (toProcess.isEmpty()) return;

    processFilesWithProgress(
        GuiceBundle.message("progress.updating.guice.model"),
        toProcess
    );
  }
  /**
   * Processes a set of files with a background progress indicator.
   * Delegates to the coroutine-based progress API via
   * {@link GuiceProgressUtilKt#processFilesWithProgressBlocking}.
   *
   * @param title the progress bar title (e.g., "Building Guice model…")
   * @param files the files to process
   */
  private void processFilesWithProgress(@NotNull String title, @NotNull Set<VirtualFile> files) {
    GuiceProgressUtilKt.processFilesWithProgressBlocking(
        myProject, title, files, this::processFile
    );
  }

  // -----------------------------------------------------------------------
  // Internal: per-file processing
  // -----------------------------------------------------------------------

  /**
   * Extracts all injection points and {@code @Provides} methods from a single file
   * by walking its top-level classes (and recursing into inner classes).
   *
   * @param file     the PSI file to extract from
   * @param ips      output set for injection-point descriptors
   * @param provides output list for {@code @Provides} descriptors
   */
  static void extractGuiceElements(@NotNull PsiFile file,
                                   @NotNull Set<InjectionPointDescriptor> ips,
                                   @NotNull List<GuiceProvides> provides) {
    if (!(file instanceof PsiClassOwner classOwner)) return;
    for (PsiClass cls : classOwner.getClasses()) {
      extractFromClass(cls, ips, provides);
    }
  }

  /**
   * Recursively extracts injection points and {@code @Provides} methods from a single
   * class declaration, including inner classes.
   *
   * <p>Injection points include:
   * <ul>
   *   <li>{@code @Inject} fields</li>
   *   <li>Parameters of {@code @Inject} methods</li>
   *   <li>Parameters of {@code @Provides} / {@code @CheckedProvides} methods</li>
   *   <li>Parameters of {@code @Inject} constructors</li>
   *   <li>Binding-call IPs from Guice module classes (via
   *       {@link GuiceInjectionUtil#getInjectionPoints(PsiClass, boolean)})</li>
   * </ul>
   *
   * @param cls      the class to extract from
   * @param ips      output set for injection-point descriptors
   * @param provides output list for {@code @Provides} descriptors
   */
  private static void extractFromClass(@NotNull PsiClass cls,
                                       @NotNull Set<InjectionPointDescriptor> ips,
                                       @NotNull List<GuiceProvides> provides) {
    // @Inject fields
    for (PsiField field : cls.getFields()) {
      if (AnnotationUtil.isAnnotated(field, GuiceAnnotations.INJECTS, 0)) {
        ips.add(new InjectionPointDescriptor(field));
      }
    }

    // @Inject methods and @Provides methods
    for (PsiMethod method : cls.getMethods()) {
      boolean isInject = AnnotationUtil.isAnnotated(method, GuiceAnnotations.INJECTS, 0);
      boolean isProvides = AnnotationUtil.isAnnotated(method, GuiceAnnotations.ALL_PROVIDES_ANNOTATIONS, 0);
      if (isInject || isProvides) {
        for (PsiParameter param : method.getParameterList().getParameters()) {
          ips.add(new InjectionPointDescriptor(param));
        }
      }
      // Track @Provides methods (all variants including multibinder annotations)
      if (isProvides) {
        provides.add(new GuiceProvides(method));
      }
    }

    // @Inject constructors
    for (PsiMethod ctor : cls.getConstructors()) {
      if (AnnotationUtil.isAnnotated(ctor, GuiceAnnotations.INJECTS, 0)) {
        for (PsiParameter param : ctor.getParameterList().getParameters()) {
          ips.add(new InjectionPointDescriptor(param));
        }
      }
    }

    // Binding-call IPs from Guice module classes (getProvider(), .to(), etc.)
    if (InheritanceUtil.isInheritor(cls, "com.google.inject.Module")) {
      Set<InjectionPointDescriptor> classIps = GuiceInjectionUtil.getInjectionPoints(cls, false);
      ips.addAll(classIps);
    }

    // Recurse into inner classes
    for (PsiClass inner : cls.getInnerClasses()) {
      extractFromClass(inner, ips, provides);
    }
  }
}
