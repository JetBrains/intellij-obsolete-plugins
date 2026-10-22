// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.guice.model;

import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer;
import com.intellij.guice.constants.GuiceAnnotations;
import com.intellij.guice.model.extensions.GuiceBindingContributor;
import com.intellij.guice.model.extensions.GuiceExtensionIndex;
import com.intellij.ide.highlighter.JavaClassFileType;
import com.intellij.java.library.JavaLibraryUtil;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileTypes.FileTypeRegistry;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassOwner;
import com.intellij.psi.PsiCompiledElement;
import com.intellij.psi.PsiCompiledFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiReference;
import com.intellij.psi.PsiTreeChangeAdapter;
import com.intellij.psi.PsiTreeChangeEvent;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.searches.AnnotatedElementsSearch;
import com.intellij.psi.search.searches.ReferencesSearch;
import com.intellij.testFramework.LightVirtualFile;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import kotlinx.coroutines.CoroutineScope;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.TestOnly;
import com.intellij.openapi.util.Disposer;
import java.util.function.Predicate;
import com.intellij.openapi.application.ReadAction;

/**
 * Project-level service that owns the Guice index infrastructure and keeps it up-to-date
 * via surgical per-file updates.
 *
 * <p>Provides the {@link GuiceNavigationIndex} as the query API for navigation.
 *
 * <h3>Design — asynchronous incremental updates</h3>
 * <ul>
 *   <li><b>Single index</b>: A {@link GuiceNavigationIndex} is created once and updated
 *       in place per file. Each file's {@link GuiceEntry} set is extracted independently
 *       by {@link GuiceEntryProducer} and committed atomically once extraction completes.</li>
 *   <li><b>Background dirty processing</b>: When a file changes (notified by {@link GuiceVfsListener}
 *       for VFS events or {@link GuicePsiTreeChangeListener} for in-memory PSI edits),
 *       it is added to a dirty set. A debounced background task processes dirty files
 *       one file per read action, then triggers re-highlighting via {@code DaemonCodeAnalyzer.restart()}.</li>
 *   <li><b>Initial population</b>: On first access (or after a structure change), background
 *       population runs fine-grained discovery steps and per-file extraction, skipping files
 *       whose modification stamps and structure generation are already up to date.</li>
 *   <li><b>Atomic per-file commits</b>: All cancellable PSI and UAST reads happen before
 *       mutating {@link GuiceNavigationIndex} or stamp maps, so a read-action cancellation
 *       never leaves the index partially updated.</li>
 * </ul>
 *
 * @see GuiceNavigationIndex
 * @see GuiceVfsListener
 */
@Service(Service.Level.PROJECT)
public final class GuiceProjectModel implements Disposable {

  @FunctionalInterface
  interface DiscoveryStep {
    @NotNull Set<VirtualFile> discover();
  }

  private record FileStamp(long vfsStamp, long psiStamp, int generation) {}

  private final Project myProject;

  /**
   * The unified navigation index — provides symmetric navigation guarantees.
   * Populated per file during file processing.
   * Thread-safe for concurrent reads and writes (uses {@link java.util.concurrent.locks.ReadWriteLock}).
   */
  private final GuiceNavigationIndex myNavigationIndex = new GuiceNavigationIndex();

  /**
   * Files marked dirty by {@link GuiceVfsListener} or {@link GuicePsiTreeChangeListener}.
   * Processed asynchronously on a background thread via {@link #myBackgroundUpdater}.
   */
  private final Set<VirtualFile> myDirtyFiles = ConcurrentHashMap.newKeySet();

  /**
   * Set to {@code true} when the project structure changes (modules, libraries,
   * or content/source roots added or removed). This triggers a re-population
   * scheduled via {@link #myBackgroundUpdater}.
   */
  private volatile boolean myStructureChanged = true;

  /**
   * Counts structure changes. A population records the value when it starts.
   * If the value changed while the population ran, the population does not clear {@link #myStructureChanged}.
   */
  private final AtomicInteger myStructureGeneration = new AtomicInteger();

  /**
   * Whether the initial population has been performed. Set to {@code true} after
   * {@link #completePopulationIfCurrent} succeeds.
   */
  private volatile boolean myInitialized = false;

  /**
   * Set once the index has been requested for the first time, and cleared only in {@link #dispose()}.
   * Before the first request, the VFS and PSI listeners can ignore events because the initial
   * population discovers all files. After it, the listeners must never go inactive again:
   * a file that a running population has already processed is not revisited, so an edit
   * dropped by an inactive listener would stay lost.
   */
  private volatile boolean myIndexRequested = false;

  /**
   * Maps file paths to their {@link VirtualFile} instances for fast lookup of dependent registrar
   * and target files across both local and in-memory file systems.
   */
  private final ConcurrentHashMap<String, VirtualFile> myFilesByPath = new ConcurrentHashMap<>();

  /**
   * Per-file VFS, PSI, and structure-generation stamps keyed by file path.
   * Updated atomically when a file's extraction result is committed to {@link #myNavigationIndex}.
   */
  private final ConcurrentHashMap<String, FileStamp> myFileStamps = new ConcurrentHashMap<>();

  /**
   * Handles debounced background processing of dirty files and initial population.
   */
  private final GuiceBackgroundIndexUpdater myBackgroundUpdater;

  /**
   * Type FQNs whose entries changed since the last restart request. The files with entries of these
   * types may show different counterparts, so {@link #requestRestartOfAffectedEditors} re-highlights them.
   */
  private final Set<String> myPendingRestartTypeFqns = ConcurrentHashMap.newKeySet();

  /**
   * Files to re-highlight when the current daemon cycle finishes. Filled by {@link #ensureFileCurrent},
   * which runs inside a highlighting pass: a restart from there would cancel the pass that just
   * computed fresh gutter icons. Drained by the {@link DaemonCodeAnalyzer.DaemonListener} of this service.
   */
  private final Set<VirtualFile> myFilesToRestartWhenDaemonFinishes = ConcurrentHashMap.newKeySet();

  // -----------------------------------------------------------------------
  // Construction / service access
  // -----------------------------------------------------------------------

  public GuiceProjectModel(@NotNull Project project, @NotNull CoroutineScope coroutineScope) {
    myProject = project;
    myBackgroundUpdater = new GuiceBackgroundIndexUpdater(project, this, coroutineScope);
    PsiManager.getInstance(myProject).addPsiTreeChangeListener(new GuicePsiTreeChangeListener(), this);
    GuiceBindingContributor.EP_NAME.addChangeListener(coroutineScope, this::onContributorsChanged);
    myProject.getMessageBus().connect(this).subscribe(DaemonCodeAnalyzer.DAEMON_EVENT_TOPIC, new DaemonCodeAnalyzer.DaemonListener() {
      @Override
      public void daemonFinished(@NotNull Collection<? extends FileEditor> fileEditors) {
        restartDeferredFiles();
      }
    });
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

  @NotNull GuiceBackgroundIndexUpdater getBackgroundUpdater() {
    return myBackgroundUpdater;
  }

  /**
   * Returns {@code true} once the index has been requested for the first time.
   * Stays {@code true} until {@link #dispose()}, see {@link #myIndexRequested}.
   */
  boolean isIndexActive() {
    return myIndexRequested;
  }

  // -----------------------------------------------------------------------
  // Main query API
  // -----------------------------------------------------------------------

  /**
   * Returns the unified navigation index. This method <b>never blocks</b> in production mode.
   *
   * <p>If the index is not yet populated (first access or structure change),
   * it requests background population and returns the (possibly empty) index.
   * Repeated requests are conflated by {@link GuiceBackgroundIndexUpdater}.
   * The background task will trigger re-highlighting when it completes.
   *
   * @param module the IntelliJ module (used for scoping initial population)
   * @return the navigation index (may be empty if population is in progress)
   */
  public @NotNull GuiceNavigationIndex getNavigationIndex(@NotNull Module module) {
    myIndexRequested = true;
    if (isPopulationNeeded()) {
      myBackgroundUpdater.scheduleInitialPopulation(module);
    }
    if (myBackgroundUpdater.isSynchronous()) {
      myBackgroundUpdater.processDirtyFilesNow();
    }
    return myNavigationIndex;
  }

  private @Nullable VirtualFile resolveVirtualFile(@NotNull String path) {
    VirtualFile vf = myFilesByPath.get(path);
    if (vf != null) return vf;
    return LocalFileSystem.getInstance().findFileByPath(path);
  }

  /**
   * Checks whether a modified {@link PsiFile} is a physical, in-content project source file
   * whose changes should update the Guice navigation index.
   */
  private boolean isIndexableProjectFile(@NotNull PsiFile file) {
    if (!file.isPhysical() || !file.getViewProvider().isEventSystemEnabled() || file.getOriginalFile() != file) {
      return false;
    }
    VirtualFile vf = file.getVirtualFile();
    return vf != null &&
           !(vf instanceof LightVirtualFile) &&
           GuiceVfsListener.isRelevantFile(vf) &&
           ProjectFileIndex.getInstance(myProject).isInContent(vf);
  }

  /**
   * Tells if the Guice features apply to a module.
   *
   * <p>A module qualifies when Guice itself ({@code com.google.inject.Inject}) is on its classpath, or when
   * it is an API-only module: it has a JSR-330 {@code Inject} ({@code javax.inject} or {@code jakarta.inject})
   * and some module of the project has Guice. Such a module declares injection points that a Guice module of
   * the project binds. A project without Guice anywhere gets no Guice features, whatever JSR-330 library it has.
   *
   * <p>The checks are the cached library-class look-ups of {@link JavaLibraryUtil}; the service does not need
   * to exist, so a caller can test the module before it creates the service.
   */
  public static boolean isGuiceAvailable(@NotNull Module module) {
    if (JavaLibraryUtil.hasLibraryClass(module, GuiceAnnotations.INJECT)) {
      return true;
    }
    boolean hasJsr330 = JavaLibraryUtil.hasLibraryClass(module, GuiceAnnotations.JAVAX_INJECT)
                        || JavaLibraryUtil.hasLibraryClass(module, GuiceAnnotations.JAKARTA_INJECT);
    return hasJsr330 && JavaLibraryUtil.hasLibraryClass(module.getProject(), GuiceAnnotations.INJECT);
  }

  // -----------------------------------------------------------------------
  // Event API — called by GuiceVfsListener and GuicePsiTreeChangeListener
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
    myFilesByPath.put(file.getPath(), file);
    myDirtyFiles.add(file);
    myBackgroundUpdater.scheduleDirtyProcessing();
  }

  /**
   * Removes a file's or directory's data entirely (when deleted, moved, or renamed).
   *
   * <p>Immediately removes the file's contributions (and any indexed child files when
   * {@code file} is a directory) from the navigation index and clears stamp tracking.
   *
   * @param file the file or directory whose old path should be removed
   * @return {@code true} if the index changed
   */
  boolean removeFile(@NotNull VirtualFile file) {
    String path = file.getPath();
    Set<String> dependentRegistrars =
        myNavigationIndex.getDependentRegistrarFilesForPathOrPrefix(path, file.isDirectory());

    boolean changed = recordUpdate(myNavigationIndex.removeFile(path));
    myFilesByPath.remove(path);
    myFileStamps.remove(path);
    myDirtyFiles.remove(file);

    if (file.isDirectory()) {
      String prefix = path + "/";
      for (String indexedPath : myNavigationIndex.getIndexedFiles()) {
        if (indexedPath.startsWith(prefix)) {
          changed |= recordUpdate(myNavigationIndex.removeFile(indexedPath));
        }
      }
      myFilesByPath.keySet().removeIf(p -> p.startsWith(prefix));
      myFileStamps.keySet().removeIf(p -> p.startsWith(prefix));
      myDirtyFiles.removeIf(vf -> vf.getPath().startsWith(prefix));
    }

    for (String registrarPath : dependentRegistrars) {
      if (registrarPath.equals(path) || (file.isDirectory() && registrarPath.startsWith(path + "/"))) {
        continue;
      }
      myFileStamps.remove(registrarPath);
      VirtualFile registrarVf = resolveVirtualFile(registrarPath);
      if (registrarVf != null && registrarVf.isValid()) {
        markFileDirty(registrarVf);
      }
    }
    if (changed) {
      // Let the background updater re-highlight the files that lost counterparts.
      myBackgroundUpdater.scheduleDirtyProcessing();
    }
    return changed;
  }

  /**
   * Records the affected types of an index update for the next {@link #requestRestartOfAffectedEditors}.
   *
   * @return whether the update changed the index
   */
  private boolean recordUpdate(@NotNull GuiceNavigationIndex.FileUpdate update) {
    if (update.changed()) {
      myPendingRestartTypeFqns.addAll(update.affectedTypeFqns());
    }
    return update.changed();
  }

  /**
   * Signals that the project structure has changed (e.g., libraries or modules
   * were added/removed). The next {@link #getNavigationIndex(Module)} call will
   * request a re-population of the index in the background.
   *
   * <p>Called by the workspace-model subscription of {@link GuiceBackgroundIndexUpdater} when the workspace
   * model reports changes to {@code LibraryEntity}, {@code ModuleEntity}, {@code ContentRootEntity},
   * or {@code SourceRootEntity}.
   */
  void markStructureChanged() {
    myStructureGeneration.incrementAndGet();
    myStructureChanged = true;
    if (!ApplicationManager.getApplication().isUnitTestMode() && !myProject.isDisposed()) {
      DaemonCodeAnalyzer.getInstance(myProject).restart("Guice project structure changed");
    }
  }

  /**
   * Reacts to a {@link GuiceBindingContributor} added or removed by a dynamic plugin.
   *
   * <p>The entries of the index hold objects of the contributors, for example the text providers of
   * {@link GuiceEntry}. Dropping the index only at the next population would keep an unloaded plugin's
   * classes, and with them its class loader, alive until then. So the index is cleared at once, and the
   * next request repopulates it from the new set of contributors. The extension point calls this listener
   * synchronously after the platform cleared the cached {@link com.intellij.guice.model.extensions.GuiceExtensionIndex}.
   */
  private void onContributorsChanged() {
    // Read action first, then the index lock: entry equality takes a read action (smart pointers).
    ReadAction.run(myNavigationIndex::clear);
    myFileStamps.clear();
    myPendingRestartTypeFqns.clear();
    myFilesToRestartWhenDaemonFinishes.clear();
    markStructureChanged();
  }

  // -----------------------------------------------------------------------
  // Disposable
  // -----------------------------------------------------------------------

  @Override
  public void dispose() {
    clearState();
  }

  /**
   * Forgets every indexed file and every pending update, as if the project had just opened. A test calls it when
   * the VFS events of its own set-up must not count as the initial discovery.
   */
  @TestOnly
  public void resetForTests() {
    clearState();
  }

  private void clearState() {
    myNavigationIndex.clear();
    myFileStamps.clear();
    myDirtyFiles.clear();
    myFilesByPath.clear();
    myInitialized = false;
    myIndexRequested = false;
  }

  // -----------------------------------------------------------------------
  // Internal: helpers for GuiceBackgroundIndexUpdater
  // -----------------------------------------------------------------------

  /**
   * Outcome of a population pass.
   *
   * @param completed    whether the pass finished for the latest structure generation
   * @param indexChanged whether the pass removed stale files from the index
   */
  record PopulationOutcome(boolean completed, boolean indexChanged) {}

  int getStructureGeneration() {
    return myStructureGeneration.get();
  }

  boolean isPopulationNeeded() {
    return !myInitialized || myStructureChanged;
  }

  boolean hasDirtyFiles() {
    return !myDirtyFiles.isEmpty();
  }

  @NotNull Set<String> getIndexedFilesSnapshot() {
    return myNavigationIndex.getIndexedFiles();
  }

  /**
   * Finishes a population pass if no newer structure change occurred while it ran.
   * Prunes the files of {@code pathsBeforePopulation} that the pass did not reach, and marks population
   * complete for {@code generation}.
   *
   * <p>A file is kept when discovery listed it in {@code processedFiles}, or when its {@link FileStamp}
   * carries {@code generation}: the dirty drain and {@link #ensureFileCurrent} process a file that was
   * edited during the pass with the current generation, and such a file is current although discovery
   * did not list it.
   */
  @NotNull PopulationOutcome completePopulationIfCurrent(int generation,
                                                         @NotNull Set<String> pathsBeforePopulation,
                                                         @NotNull Set<VirtualFile> processedFiles) {
    if (generation != myStructureGeneration.get()) {
      return new PopulationOutcome(false, false);
    }
    Set<String> keep = new HashSet<>();
    for (VirtualFile file : processedFiles) {
      keep.add(file.getPath());
    }
    boolean changed = false;
    for (String path : pathsBeforePopulation) {
      if (keep.contains(path) || isStampedWithGeneration(path, generation)) continue;
      changed |= recordUpdate(myNavigationIndex.removeFile(path));
      myFileStamps.remove(path);
    }
    if (generation == myStructureGeneration.get()) {
      myStructureChanged = false;
      myInitialized = true;
      return new PopulationOutcome(true, changed);
    }
    return new PopulationOutcome(false, changed);
  }

  private boolean isStampedWithGeneration(@NotNull String path, int generation) {
    FileStamp stamp = myFileStamps.get(path);
    return stamp != null && stamp.generation() == generation;
  }

  /**
   * Builds a list of fine-grained discovery steps across all Guice-enabled project modules.
   * Each step executes a single index search inside its own read action so write actions
   * can interleave without restarting completed searches from the beginning.
   */
  @NotNull List<DiscoveryStep> createDiscoverySteps(@NotNull Module module) {
    Set<Module> guiceModules = new HashSet<>();
    // The module that requested the index may be gone when a background pass reaches this point.
    if (!module.isDisposed()) {
      guiceModules.add(module);
    }
    for (Module m : ModuleManager.getInstance(myProject).getModules()) {
      if (isGuiceAvailable(m)) {
        guiceModules.add(m);
      }
    }
    GlobalSearchScope unionScope = GlobalSearchScope.EMPTY_SCOPE;
    for (Module m : guiceModules) {
      unionScope = unionScope.union(GlobalSearchScope.moduleWithDependenciesAndLibrariesScope(m));
    }
    final GlobalSearchScope scope = unionScope;
    GuiceExtensionIndex extensionIndex = GuiceExtensionIndex.get();
    List<DiscoveryStep> steps = new ArrayList<>();

    // 1. Files with @Inject fields/methods (one step per annotation FQN)
    for (String injectAnno : GuiceAnnotations.INJECTS) {
      steps.add(() -> discoverFieldsAndMethodsByAnnotation(injectAnno, scope));
    }

    // 2. Files with @Provides* method annotations
    for (String providesAnno : extensionIndex.getAllProvidesAnnotations()) {
      steps.add(() -> discoverMethodsByAnnotation(providesAnno, scope));
    }

    // 3. Classes with @ImplementedBy or @ProvidedBy
    for (String jitAnno : List.of(GuiceAnnotations.IMPLEMENTED_BY, GuiceAnnotations.PROVIDED_BY)) {
      steps.add(() -> discoverClassesByAnnotation(jitAnno, scope));
    }

    // 4. Guice module files (for bindings defined in configure())
    steps.add(() -> {
      Set<VirtualFile> files = new LinkedHashSet<>();
      for (PsiClass cls : GuiceInjectorManager.getGuiceModuleClasses(module, scope)) {
        addFileOf(cls, files);
      }
      recordDiscoveredFiles(files);
      return files;
    });

    // 5. Files calling contributor-registered global binder helpers outside Module classes
    for (String ownerFqn : extensionIndex.getGlobalCallOwnerClasses()) {
      steps.add(() -> discoverClassReferences(ownerFqn, scope));
    }

    return steps;
  }

  private @NotNull Set<VirtualFile> discoverFieldsAndMethodsByAnnotation(@NotNull String annoFqn,
                                                                         @NotNull GlobalSearchScope scope) {
    PsiClass annoClass = JavaPsiFacade.getInstance(myProject).findClass(annoFqn, GlobalSearchScope.allScope(myProject));
    if (annoClass == null) return Set.of();
    Set<VirtualFile> files = new LinkedHashSet<>();
    for (PsiField field : AnnotatedElementsSearch.searchPsiFields(annoClass, scope).findAll()) {
      addFileOf(field, files);
    }
    for (PsiMethod method : AnnotatedElementsSearch.searchPsiMethods(annoClass, scope).findAll()) {
      addFileOf(method, files);
    }
    recordDiscoveredFiles(files);
    return files;
  }

  private @NotNull Set<VirtualFile> discoverMethodsByAnnotation(@NotNull String annoFqn,
                                                                @NotNull GlobalSearchScope scope) {
    PsiClass annoClass = JavaPsiFacade.getInstance(myProject).findClass(annoFqn, GlobalSearchScope.allScope(myProject));
    if (annoClass == null) return Set.of();
    Set<VirtualFile> files = new LinkedHashSet<>();
    for (PsiMethod method : AnnotatedElementsSearch.searchPsiMethods(annoClass, scope).findAll()) {
      addFileOf(method, files);
    }
    recordDiscoveredFiles(files);
    return files;
  }

  private @NotNull Set<VirtualFile> discoverClassesByAnnotation(@NotNull String annoFqn,
                                                                @NotNull GlobalSearchScope scope) {
    PsiClass annoClass = JavaPsiFacade.getInstance(myProject).findClass(annoFqn, GlobalSearchScope.allScope(myProject));
    if (annoClass == null) return Set.of();
    Set<VirtualFile> files = new LinkedHashSet<>();
    for (PsiClass cls : AnnotatedElementsSearch.searchPsiClasses(annoClass, scope).findAll()) {
      addFileOf(cls, files);
    }
    recordDiscoveredFiles(files);
    return files;
  }

  private @NotNull Set<VirtualFile> discoverClassReferences(@NotNull String ownerFqn,
                                                            @NotNull GlobalSearchScope scope) {
    PsiClass ownerClass = JavaPsiFacade.getInstance(myProject).findClass(ownerFqn, GlobalSearchScope.allScope(myProject));
    if (ownerClass == null) return Set.of();
    Set<VirtualFile> files = new LinkedHashSet<>();
    for (PsiReference ref : ReferencesSearch.search(ownerClass, scope).findAll()) {
      addFileOf(ref.getElement(), files);
    }
    recordDiscoveredFiles(files);
    return files;
  }

  private void recordDiscoveredFiles(@NotNull Set<VirtualFile> files) {
    for (VirtualFile vf : files) {
      myFilesByPath.put(vf.getPath(), vf);
    }
  }

  /**
   * Adds the containing virtual file of a PSI element to {@code files} without mutating model state.
   * For compiled {@code .class} files, prefers the attached source file when available.
   */
  private static void addFileOf(@NotNull PsiElement element, @NotNull Set<VirtualFile> files) {
    PsiFile file = element.getContainingFile();
    if (file == null) return;

    if (file instanceof PsiCompiledFile clsFile) {
      PsiElement sourceNav = clsFile.getNavigationElement();
      if (sourceNav instanceof PsiFile sourceFile && sourceNav != clsFile) {
        VirtualFile sourceVf = sourceFile.getVirtualFile();
        if (sourceVf != null) {
          files.add(sourceVf);
          return;
        }
      }
    }

    VirtualFile vf = file.getVirtualFile();
    if (vf != null) {
      files.add(vf);
    }
  }

  // -----------------------------------------------------------------------
  // Internal: atomic per-file processing
  // -----------------------------------------------------------------------

  /**
   * Extracts Guice entries for a discovered file if its stamp or structure generation changed.
   *
   * @return {@code true} if the index changed
   */
  boolean processDiscoveredFile(@NotNull VirtualFile vf, int generation) {
    return processFileAtomically(vf, generation, false);
  }

  /**
   * Returns a snapshot of the current dirty files without mutating state.
   */
  @NotNull List<VirtualFile> collectDirtyFiles() {
    if (myDirtyFiles.isEmpty()) return List.of();
    return new ArrayList<>(myDirtyFiles);
  }

  /**
   * Processes a single dirty file atomically and removes it from the dirty set once extraction completes.
   * Any dependent registrar files are invalidated and queued into {@link #myDirtyFiles}.
   *
   * <p>Must be called under a read action.
   *
   * @param file the file to process
   * @return {@code true} if the index changed, {@code false} if the file stamp was unchanged
   * or the new extraction result equals the stored one
   */
  boolean processSingleDirtyFile(@NotNull VirtualFile file) {
    try {
      return processFileAtomically(file, myStructureGeneration.get(), true);
    }
    catch (Throwable t) {
      GuiceExtensionIndex.rethrowIfControlFlow(t);
      // The file leaves the dirty set, or the drain would loop on it forever. Its stamp is not recorded,
      // so the next highlighting pass or edit of the file retries the extraction.
      myDirtyFiles.remove(file);
      throw t;
    }
  }

  private static volatile @Nullable Predicate<VirtualFile> ourExtractionFailureForTests;

  /**
   * Makes the extraction of the files that match {@code failing} throw, to test the failure paths of the
   * update pipeline.
   */
  @TestOnly
  public static void setExtractionFailureForTests(@NotNull Predicate<VirtualFile> failing, @NotNull Disposable parent) {
    ourExtractionFailureForTests = failing;
    Disposer.register(parent, () -> ourExtractionFailureForTests = null);
  }

  /**
   * Performs a two-phase atomic update for a single file:
   * <ol>
   *   <li><b>Phase 1 (Read-only, cancellable)</b>: Resolves the PSI file, checks the {@link FileStamp},
   *       and extracts {@link GuiceEntryProducer.FileExtractionResult} into local variables without mutating state.</li>
   *   <li><b>Phase 2 (In-memory commit, non-cancellable)</b>: Updates {@link #myNavigationIndex},
   *       {@link #myFileStamps}, and {@link #myDirtyFiles} together, and queues any dependent registrars.</li>
   * </ol>
   *
   * @return {@code true} if the index changed. A changed stamp with an equal extraction result
   * (for example an edit in a comment) returns {@code false}, so the caller does not restart highlighting.
   */
  private boolean processFileAtomically(@NotNull VirtualFile vf, int generation, boolean fromDirtySet) {
    // Phase 1: Pure read-only checks and PSI/UAST extraction (cancellable)
    if (!vf.isValid()) {
      return removeFile(vf);
    }
    String path = vf.getPath();
    PsiFile psiFile = PsiManager.getInstance(myProject).findFile(vf);
    if (psiFile == null) {
      return removeFile(vf);
    }

    PsiFile fileForExtraction = psiFile;
    boolean isCompiled = psiFile instanceof PsiCompiledElement
                         || FileTypeRegistry.getInstance().isFileOfType(vf, JavaClassFileType.INSTANCE);
    if (isCompiled) {
      PsiElement sourceNav = psiFile.getNavigationElement();
      if (sourceNav instanceof PsiFile sourceFile && sourceFile != psiFile) {
        fileForExtraction = sourceFile;
      }
      else if (!ProjectFileIndex.getInstance(myProject).isInProject(vf)) {
        return removeFile(vf);
      }
    }

    FileStamp currentStamp = new FileStamp(
        vf.getModificationStamp(),
        fileForExtraction.getModificationStamp(),
        generation
    );
    FileStamp recordedStamp = myFileStamps.get(path);
    if (currentStamp.equals(recordedStamp)) {
      if (fromDirtySet) {
        myDirtyFiles.remove(vf);
      }
      return false;
    }

    Predicate<VirtualFile> failureForTests = ourExtractionFailureForTests;
    if (failureForTests != null && failureForTests.test(vf)) {
      throw new IllegalStateException("Simulated extraction failure for " + path);
    }
    GuiceEntryProducer.FileExtractionResult result =
        fileForExtraction instanceof PsiClassOwner
        ? GuiceEntryProducer.extractFileData(fileForExtraction)
        : GuiceEntryProducer.FileExtractionResult.EMPTY;

    // A structure or contributor change during Phase 1 invalidates the result: the entries may hold objects
    // of a removed contributor. Leave the file for the next population or drain, which uses the new generation.
    if (generation != myStructureGeneration.get()) {
      return false;
    }

    // Phase 2: Atomic in-memory commit (zero PSI access, zero cancellation points)
    myFilesByPath.put(path, vf);
    for (VirtualFile targetVf : result.referencedTargetFiles()) {
      myFilesByPath.put(targetVf.getPath(), targetVf);
    }
    boolean changed = recordUpdate(myNavigationIndex.updateFile(path, result));
    myFileStamps.put(path, currentStamp);
    if (fromDirtySet) {
      myDirtyFiles.remove(vf);
    }

    // Queue the registrar files that read this file, even when this file's own entries did not change:
    // a registrar extracts its entries from the PSI of this file, so any edit here can change them.
    if (fromDirtySet || recordedStamp != null) {
      Set<String> dependentRegistrars = new LinkedHashSet<>(myNavigationIndex.getDependentRegistrarFiles(path));
      dependentRegistrars.addAll(myNavigationIndex.getDependentRegistrarFilesByClassName(vf.getNameWithoutExtension()));
      dependentRegistrars.remove(path);
      for (String registrarPath : dependentRegistrars) {
        myFileStamps.remove(registrarPath);
        VirtualFile registrarVf = myFilesByPath.get(registrarPath);
        if (registrarVf != null) {
          myDirtyFiles.add(registrarVf);
        }
      }
    }
    return changed;
  }

  // -----------------------------------------------------------------------
  // Current-file freshness for line marker providers
  // -----------------------------------------------------------------------

  /**
   * Brings the index up to date for the file of a running highlighting pass.
   *
   * <p>The line marker providers call it first, so the first pass after an edit already shows the new
   * gutter icons instead of lagging one pass behind the background update. The work is the same
   * two-phase atomic update that the background uses; it removes the file from the dirty set, so the
   * background does not repeat it. A file that no longer differs from its recorded stamp costs two map
   * lookups. Other open files whose counterparts changed are re-highlighted when the current daemon cycle
   * finishes, so this pass is not cancelled.
   *
   * <p>Must be called under a read action.
   */
  void ensureFileCurrent(@NotNull PsiFile psiFile) {
    if (!isIndexActive() || !isIndexableProjectFile(psiFile)) return;
    VirtualFile vf = psiFile.getVirtualFile();
    if (!myDirtyFiles.contains(vf)) {
      FileStamp recordedStamp = myFileStamps.get(vf.getPath());
      if (recordedStamp != null
          && recordedStamp.vfsStamp() == vf.getModificationStamp()
          && recordedStamp.psiStamp() == psiFile.getModificationStamp()
          && recordedStamp.generation() == myStructureGeneration.get()) {
        return;
      }
    }
    if (processSingleDirtyFile(vf)) {
      requestRestartOfAffectedEditors(RestartMode.WHEN_DAEMON_FINISHES, vf);
    }
  }

  // -----------------------------------------------------------------------
  // Targeted re-highlighting
  // -----------------------------------------------------------------------

  enum RestartMode {
    /** Restart now. For updates that run outside a highlighting pass. */
    IMMEDIATELY,
    /** Restart after the running daemon cycle. For updates that run inside a highlighting pass. */
    WHEN_DAEMON_FINISHES
  }

  boolean hasPendingRestarts() {
    return !myPendingRestartTypeFqns.isEmpty();
  }

  @TestOnly
  boolean hasDeferredRestart(@NotNull VirtualFile file) {
    return myFilesToRestartWhenDaemonFinishes.contains(file);
  }

  /**
   * Re-highlights the open files whose gutter icons may show different counterparts after the index
   * updates since the previous call. Only the files with entries of the affected types are restarted,
   * instead of every open editor.
   *
   * @param excludedFile a file that is current already, for example the file of the running pass
   */
  void requestRestartOfAffectedEditors(@NotNull RestartMode mode, @Nullable VirtualFile excludedFile) {
    if (myPendingRestartTypeFqns.isEmpty() || myProject.isDisposed()) return;
    Set<String> typeFqns = new HashSet<>(myPendingRestartTypeFqns);
    myPendingRestartTypeFqns.removeAll(typeFqns);

    Set<VirtualFile> files = myNavigationIndex.getFilesWithEntriesOfTypes(typeFqns);
    // An open file that declares an affected type holds no entry of its own when the type is an interface
    // or has no @Inject member, but its class gutter shows the bindings of the type.
    files.addAll(openFilesDeclaringTypes(typeFqns));
    if (excludedFile != null) {
      files.remove(excludedFile);
    }
    if (files.isEmpty()) return;

    if (mode == RestartMode.WHEN_DAEMON_FINISHES) {
      myFilesToRestartWhenDaemonFinishes.addAll(files);
    }
    else {
      ApplicationManager.getApplication().invokeLater(() -> restartOpenFiles(files), myProject.getDisposed());
    }
  }

  /**
   * Returns the open files that declare a class whose qualified name is in {@code typeFqns}. Reads the
   * class names from the stubs; open editors are few, so this is cheap.
   */
  private @NotNull Set<VirtualFile> openFilesDeclaringTypes(@NotNull Set<String> typeFqns) {
    if (myProject.isDisposed()) return Set.of();
    VirtualFile[] openFiles = FileEditorManager.getInstance(myProject).getOpenFiles();
    if (openFiles.length == 0) return Set.of();
    return ReadAction.compute(() -> {
      Set<VirtualFile> result = new HashSet<>();
      PsiManager psiManager = PsiManager.getInstance(myProject);
      for (VirtualFile openFile : openFiles) {
        if (!openFile.isValid() || !GuiceVfsListener.isRelevantFile(openFile)) continue;
        PsiFile psiFile = psiManager.findFile(openFile);
        if (psiFile instanceof PsiClassOwner classOwner && declaresAnyOf(classOwner.getClasses(), typeFqns)) {
          result.add(openFile);
        }
      }
      return result;
    });
  }

  private static boolean declaresAnyOf(PsiClass @NotNull [] classes, @NotNull Set<String> typeFqns) {
    for (PsiClass psiClass : classes) {
      String fqn = psiClass.getQualifiedName();
      if (fqn != null && typeFqns.contains(fqn)) return true;
      if (declaresAnyOf(psiClass.getInnerClasses(), typeFqns)) return true;
    }
    return false;
  }

  /**
   * Restarts the highlighting of every open file after a population pass. The targeted restart cannot name
   * the files whose gutters depend on the new index but that hold no entries themselves.
   */
  void restartAllOpenFilesAfterPopulation() {
    if (myProject.isDisposed() || isUnitTestMode()) return;
    DaemonCodeAnalyzer.getInstance(myProject).restart("Guice model populated");
  }

  private void restartDeferredFiles() {
    if (myFilesToRestartWhenDaemonFinishes.isEmpty() || isUnitTestMode()) return;
    Set<VirtualFile> files = new HashSet<>(myFilesToRestartWhenDaemonFinishes);
    myFilesToRestartWhenDaemonFinishes.removeAll(files);
    restartOpenFiles(files);
  }

  /**
   * Restarts the highlighting of the given files that are open in an editor. Runs on the EDT.
   *
   * <p>Tests drive highlighting with {@code doHighlighting()} and forbid daemon restarts during a pass,
   * so unit-test mode issues no restart (like {@link #markStructureChanged}).
   */
  private void restartOpenFiles(@NotNull Set<VirtualFile> files) {
    if (myProject.isDisposed() || isUnitTestMode()) return;
    FileEditorManager editorManager = FileEditorManager.getInstance(myProject);
    PsiManager psiManager = PsiManager.getInstance(myProject);
    DaemonCodeAnalyzer daemon = DaemonCodeAnalyzer.getInstance(myProject);
    for (VirtualFile file : files) {
      if (!file.isValid() || !editorManager.isFileOpen(file)) continue;
      PsiFile psiFile = psiManager.findFile(file);
      if (psiFile != null) {
        daemon.restart(psiFile, "Guice counterparts changed");
      }
    }
  }

  private static boolean isUnitTestMode() {
    return ApplicationManager.getApplication().isUnitTestMode();
  }

  @TestOnly
  void clearPendingRestartsForTests() {
    myPendingRestartTypeFqns.clear();
    myFilesToRestartWhenDaemonFinishes.clear();
  }

  private final class GuicePsiTreeChangeListener extends PsiTreeChangeAdapter {
    @Override
    public void childAdded(@NotNull PsiTreeChangeEvent event) {
      onPsiChange(event);
    }

    @Override
    public void childRemoved(@NotNull PsiTreeChangeEvent event) {
      onPsiChange(event);
    }

    @Override
    public void childReplaced(@NotNull PsiTreeChangeEvent event) {
      onPsiChange(event);
    }

    @Override
    public void childrenChanged(@NotNull PsiTreeChangeEvent event) {
      onPsiChange(event);
    }

    @Override
    public void childMoved(@NotNull PsiTreeChangeEvent event) {
      onPsiChange(event);
    }

    @Override
    public void propertyChanged(@NotNull PsiTreeChangeEvent event) {
      onPsiChange(event);
    }

    private void onPsiChange(@NotNull PsiTreeChangeEvent event) {
      if (myProject.isDisposed() || !myProject.isInitialized() || !isIndexActive()) return;
      PsiFile psiFile = event.getFile();
      if (psiFile == null && event.getChild() instanceof PsiFile childFile) {
        psiFile = childFile;
      }
      if (psiFile == null && event.getElement() instanceof PsiFile elementFile) {
        psiFile = elementFile;
      }
      if (psiFile != null && isIndexableProjectFile(psiFile)) {
        VirtualFile vf = psiFile.getVirtualFile();
        if (vf != null) {
          markFileDirty(vf);
        }
      }
    }
  }
}
