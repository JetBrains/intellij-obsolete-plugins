// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.guice.model;

import com.intellij.codeInsight.AnnotationUtil;
import com.intellij.guice.constants.GuiceAnnotations;
import com.intellij.guice.model.beans.BindDescriptor;
import com.intellij.guice.model.extensions.GuiceBindingMatchStrategy;
import com.intellij.guice.model.jam.GuiceProvides;
import com.intellij.lang.Language;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectFileIndex;
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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import kotlinx.coroutines.CoroutineScope;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.psi.KtFile;

/**
 * Project-level service that owns the Guice index infrastructure and keeps it up-to-date
 * via surgical per-file updates.
 *
 * <p>Provides the {@link GuiceNavigationIndex} as the primary query API for navigation.
 * The underlying {@link GuiceLiveIndex} is used as the storage/mutation layer.
 *
 * <h3>Design — asynchronous incremental updates</h3>
 * <ul>
 *   <li><b>Single live index</b>: A {@link GuiceLiveIndex} is created once and never rebuilt
 *       from scratch (unless project structure changes).  Each file's Guice data (bindings,
 *       injection points, {@code @Provides}) is extracted independently and stored per-file
 *       inside the live index.</li>
 *   <li><b>Background dirty processing</b>: When a file changes (notified by {@link GuiceVfsListener}),
 *       it is added to a dirty set.  A debounced background task processes dirty files
 *       asynchronously using {@code ReadAction.nonBlocking}, then triggers re-highlighting
 *       via {@code DaemonCodeAnalyzer.restart()}.</li>
 *   <li><b>Inline current-file re-indexing</b>: During highlighting, the annotator re-indexes
 *       the current file inline (cancellable) for immediate feedback, without waiting for
 *       the background debounced processing.</li>
 *   <li><b>Initial population</b>: On first access (or after a structure change), background
 *       population is scheduled.  The annotator receives a (possibly empty) index and gutter
 *       icons appear once population completes and re-highlighting is triggered.</li>
 *   <li><b>Modification stamps</b>: Per-file VFS modification stamps are tracked to skip
 *       spurious VFS events where the file content hasn't actually changed.</li>
 *   <li><b>Thread safety</b>: The dirty set uses {@link ConcurrentHashMap#newKeySet()}.
 *       Both {@link GuiceLiveIndex} and {@link GuiceNavigationIndex} are thread-safe.
 *       Concurrent writes from the highlighting thread and background thread are safe.</li>
 * </ul>
 *
 * @see GuiceNavigationIndex
 * @see GuiceLiveIndex
 * @see GuiceVfsListener
 */
@Service(Service.Level.PROJECT)
public final class GuiceProjectModel implements Disposable {

  private final Project myProject;

  /**
   * The live index — storage/mutation layer, never rebuilt from scratch
   * (except on structure change), only surgically updated per-file.
   */
  private final GuiceLiveIndex myLiveIndex = new GuiceLiveIndex();

  /**
   * The unified navigation index — provides symmetric navigation guarantees.
   * Populated alongside {@link #myLiveIndex} during file processing.
   * Thread-safe for concurrent reads and writes (uses {@link java.util.concurrent.locks.ReadWriteLock}).
   */
  private final GuiceNavigationIndex myNavigationIndex = new GuiceNavigationIndex();

  /**
   * Files marked dirty by {@link GuiceVfsListener}.  Processed asynchronously
   * on a background thread via {@link #myBackgroundUpdater}.
   * Uses a concurrent set for thread safety since VFS events may fire on any thread.
   */
  private final Set<VirtualFile> myDirtyFiles = ConcurrentHashMap.newKeySet();

  /**
   * Set to {@code true} when the project structure changes (modules or libraries
   * added/removed, or a file is deleted).  This triggers a full re-population
   * scheduled via {@link #myBackgroundUpdater}.
   */
  private volatile boolean myStructureChanged = true;

  /**
   * Whether the initial population has been performed.  Set to {@code true} after
   * {@link #markPopulationComplete()} is called by the background updater.
   */
  private volatile boolean myInitialized = false;

  /**
   * Guards initial population: set to true when a background population has
   * been scheduled, to prevent scheduling duplicates.
   */
  private volatile boolean myPopulationScheduled = false;

  /**
   * Per-file VFS modification stamps.  Used to detect whether a file in the dirty
   * set has actually changed (VFS may fire spurious events).
   */
  private final ConcurrentHashMap<VirtualFile, Long> myFileStamps = new ConcurrentHashMap<>();

  /**
   * Handles debounced background processing of dirty files and initial population.
   * Triggers {@link DaemonCodeAnalyzer#restart} after updates.
   */
  private final GuiceBackgroundIndexUpdater myBackgroundUpdater;

  // -----------------------------------------------------------------------
  // Construction / service access
  // -----------------------------------------------------------------------

  public GuiceProjectModel(@NotNull Project project, @NotNull CoroutineScope coroutineScope) {
    myProject = project;
    myBackgroundUpdater = new GuiceBackgroundIndexUpdater(project, this, coroutineScope);
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
   * Returns the unified navigation index.  This method <b>never blocks</b>.
   *
   * <p>If the index is not yet populated (first access or structure change),
   * it schedules background population and returns the (possibly empty) index.
   * The background task will trigger re-highlighting when it completes.
   *
   * @param module the IntelliJ module (used for scoping initial population)
   * @return the navigation index (may be empty if population is in progress)
   */
  public @NotNull GuiceNavigationIndex getNavigationIndex(@NotNull Module module) {
    if (!myInitialized || myStructureChanged) {
      if (!myPopulationScheduled) {
        myPopulationScheduled = true;
        myBackgroundUpdater.scheduleInitialPopulation(module);
      }
    }
    return myNavigationIndex;
  }

  /**
   * Re-indexes a single file inline during the highlighting pass.
   *
   * <p>This is <b>cancellable</b> — if the highlighting thread is cancelled
   * (e.g., user types), a {@link com.intellij.openapi.progress.ProcessCanceledException}
   * will be thrown and the update is abandoned. The next highlighting pass will retry.
   *
   * <p>This gives immediate feedback for the file the user is editing,
   * without waiting for the background debounced processing.
   *
   * @param file the PSI file currently being highlighted
   */
  public void reindexCurrentFile(@NotNull PsiFile file) {
    GuiceProgressUtilKt.reindexFileInline(file, myNavigationIndex);
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
   * <p>The file is added to the dirty set and background processing is
   * scheduled (debounced). When processing completes, re-highlighting
   * is triggered so gutter icons update.
   *
   * @param file the file that has changed
   */
  void markFileDirty(@NotNull VirtualFile file) {
    myDirtyFiles.add(file);
    myBackgroundUpdater.scheduleDirtyProcessing();
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
    myNavigationIndex.removeFile(file.getPath());
    myFileStamps.remove(file);
    myDirtyFiles.remove(file);
  }

  /**
   * Signals that the project structure has changed (e.g., libraries or modules
   * were added/removed).  The next {@link #getNavigationIndex(Module)} call will
   * schedule a full re-population of the index in the background.
   *
   * <p>Called by {@link GuiceWorkspaceModelListener} when the workspace model
   * reports changes to {@code LibraryEntity} or {@code ModuleEntity}.
   */
  void markStructureChanged() {
    myStructureChanged = true;
    myPopulationScheduled = false;  // Allow re-scheduling of background population.
  }

  // -----------------------------------------------------------------------
  // Disposable
  // -----------------------------------------------------------------------

  @Override
  public void dispose() {
    myLiveIndex.clear();
    myNavigationIndex.clear();
    myFileStamps.clear();
    myDirtyFiles.clear();
    myInitialized = false;
    myPopulationScheduled = false;
  }

  // -----------------------------------------------------------------------
  // Internal: helpers for GuiceBackgroundIndexUpdater
  // -----------------------------------------------------------------------

  /**
   * Clears all index data in preparation for a full re-population.
   * Called by the background updater at the start of initial population.
   */
  void clearIndices() {
    myLiveIndex.clear();
    myNavigationIndex.clear();
    myFileStamps.clear();
    myDirtyFiles.clear();
  }

  /**
   * Marks the index as fully populated and clears the structure-changed flag.
   * Called by the background updater after all files have been processed.
   */
  void markPopulationComplete() {
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
  @NotNull Set<VirtualFile> discoverRelevantFiles(@NotNull Module module) {
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
    for (String providesAnno : GuiceBindingMatchStrategy.getAllProvidesAnnotations()) {
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
   * @param snapshot pre-computed contributor state, shared across the batch
   */
  void processFile(@NotNull VirtualFile vf,
                   @NotNull GuiceInjectorManager.ContributorSnapshot snapshot) {
    PsiFile psiFile = PsiManager.getInstance(myProject).findFile(vf);
    if (psiFile == null) {
      myLiveIndex.removeFile(vf);
      myNavigationIndex.removeFile(vf.getPath());
      myFileStamps.remove(vf);
      return;
    }

    // For compiled class files, try to resolve to source for full extraction.
    PsiFile fileForExtraction = psiFile;
    boolean isCompiled = false;
    Language language = psiFile.getLanguage();
    if (psiFile instanceof ClsFileImpl) {
      isCompiled = true;
    } else if (psiFile instanceof KtFile ktFile) {
      isCompiled = ktFile.isCompiled();
    }
    if (isCompiled) {
      PsiElement sourceNav = psiFile.getNavigationElement();
      if (sourceNav instanceof PsiFile sourceFile && sourceFile != psiFile) {
        fileForExtraction = sourceFile;
        isCompiled = false; // We have source — treat as a regular source file.
      } else {
        if (!ProjectFileIndex.getInstance(myProject).isInProject(vf)) {
          // This is compiled class that is not directly owned by our project. Skip!
          myLiveIndex.removeFile(vf);
          myNavigationIndex.removeFile(vf.getPath());
          myFileStamps.remove(vf);
          return;
        }
      }
    }

    // Extract bindings (only from source files).
    // Compiled files without source cannot provide method bodies for binding extraction.
    // getBindingsInFile() internally checks for Guice module classes, so no pre-check needed.
    Set<BindDescriptor> bindings = isCompiled ? Set.of() : GuiceInjectorManager.getBindingsInFile(fileForExtraction, snapshot);

    // Extract injection points and @Provides.
    // For compiled files without source, AnnotationUtil.isAnnotated() still works
    // (annotations are preserved in bytecode), so @Inject IPs and @Provides are
    // correctly extracted.  Only binding-call IPs (getProvider(), .to()) are missed
    // since those require method body search — acceptable since library code is
    // rarely edited and these are Guice-internal patterns.
    Set<InjectionPointDescriptor> ips = new HashSet<>();
    List<GuiceProvides> provides = new ArrayList<>();
    extractGuiceElements(fileForExtraction, ips, provides, snapshot);

    // Key the index by the original VirtualFile (the one the VFS listener tracks).
    myLiveIndex.updateFile(vf, bindings, ips, provides);

    // Also populate the unified navigation index.
    Set<GuiceEntry> entries = new HashSet<>();
    if (fileForExtraction instanceof PsiClassOwner classOwner) {
      for (PsiClass cls : classOwner.getClasses()) {
        entries.addAll(GuiceEntryProducer.extractFromClass(cls));
      }
    }
    myNavigationIndex.updateFile(vf.getPath(), entries);

    myFileStamps.put(vf, vf.getModificationStamp());
  }

  /**
   * Collects and returns the list of dirty files that actually need reprocessing.
   *
   * <p>Files that are invalid (deleted) are cleaned up from the index immediately.
   * Files whose modification stamp hasn't changed are skipped.
   * The returned list contains only files that need {@link #processFile} called.
   *
   * <p>Must be called under a read action.
   *
   * @return the list of dirty files to reprocess, never {@code null}
   */
  @NotNull List<VirtualFile> collectDirtyFiles() {
    if (myDirtyFiles.isEmpty()) return List.of();

    List<VirtualFile> toProcess = new ArrayList<>();

    for (VirtualFile file : new ArrayList<>(myDirtyFiles)) {
      if (!file.isValid()) {
        myLiveIndex.removeFile(file);
        myNavigationIndex.removeFile(file.getPath());
        myFileStamps.remove(file);
        myDirtyFiles.remove(file);
        continue;
      }
      Long oldStamp = myFileStamps.get(file);
      long currentStamp = file.getModificationStamp();
      if (oldStamp != null && oldStamp == currentStamp) {
        myDirtyFiles.remove(file); // No actual change, skip.
        continue;
      }

      toProcess.add(file);
    }

    return toProcess;
  }

  /**
   * Processes a single dirty file and removes it from the dirty set.
   *
   * <p>Must be called under a read action.
   *
   * @param file     the file to process
   * @param snapshot pre-computed contributor state
   */
  void processSingleDirtyFile(@NotNull VirtualFile file,
                              @NotNull GuiceInjectorManager.ContributorSnapshot snapshot) {
    processFile(file, snapshot);
    myDirtyFiles.remove(file);
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
                                   @NotNull List<GuiceProvides> provides,
                                   @NotNull GuiceInjectorManager.ContributorSnapshot snapshot) {
    if (!(file instanceof PsiClassOwner classOwner)) return;
    for (PsiClass cls : classOwner.getClasses()) {
      extractFromClass(cls, ips, provides, snapshot);
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
                                       @NotNull List<GuiceProvides> provides,
                                       @NotNull GuiceInjectorManager.ContributorSnapshot snapshot) {
    // @Inject fields
    for (PsiField field : cls.getFields()) {
      if (AnnotationUtil.isAnnotated(field, GuiceAnnotations.INJECTS, 0)) {
        ips.add(new InjectionPointDescriptor(field));
      }
    }

    // @Inject methods and @Provides methods
    for (PsiMethod method : cls.getMethods()) {
      boolean isInject = AnnotationUtil.isAnnotated(method, GuiceAnnotations.INJECTS, 0);
      boolean isProvides = AnnotationUtil.isAnnotated(method, snapshot.providesAnnotations(), 0);
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
      extractFromClass(inner, ips, provides, snapshot);
    }
  }
}
