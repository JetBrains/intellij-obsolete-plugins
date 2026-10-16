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
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import kotlinx.coroutines.CoroutineScope;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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
   * Guards initial population: set to true when a background population has
   * been scheduled, to prevent scheduling duplicates.
   */
  private final AtomicBoolean myPopulationScheduled = new AtomicBoolean();

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
   * Triggers {@link com.intellij.codeInsight.daemon.DaemonCodeAnalyzer#restart} after updates.
   */
  private final GuiceBackgroundIndexUpdater myBackgroundUpdater;

  // -----------------------------------------------------------------------
  // Construction / service access
  // -----------------------------------------------------------------------

  public GuiceProjectModel(@NotNull Project project, @NotNull CoroutineScope coroutineScope) {
    myProject = project;
    myBackgroundUpdater = new GuiceBackgroundIndexUpdater(project, this, coroutineScope);
    PsiManager.getInstance(myProject).addPsiTreeChangeListener(new GuicePsiTreeChangeListener(), this);
    // Entries hold objects of dynamic extensions. Rebuild the index when a plugin adds or removes one.
    GuiceBindingContributor.EP_NAME.addChangeListener(coroutineScope, this::markStructureChanged);
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
   * Returns {@code true} once initial population has been scheduled or completed.
   * Before the index is first requested for a Guice-enabled module, VFS and PSI
   * listeners can ignore events because initial population will discover all files.
   */
  boolean isIndexActive() {
    return myInitialized || myPopulationScheduled.get();
  }

  // -----------------------------------------------------------------------
  // Main query API
  // -----------------------------------------------------------------------

  /**
   * Returns the unified navigation index. This method <b>never blocks</b> in production mode.
   *
   * <p>If the index is not yet populated (first access or structure change),
   * it schedules background population and returns the (possibly empty) index.
   * The background task will trigger re-highlighting when it completes.
   *
   * @param module the IntelliJ module (used for scoping initial population)
   * @return the navigation index (may be empty if population is in progress)
   */
  public @NotNull GuiceNavigationIndex getNavigationIndex(@NotNull Module module) {
    if (isPopulationNeeded() && myPopulationScheduled.compareAndSet(false, true)) {
      myBackgroundUpdater.scheduleInitialPopulation(module);
    }
    if (ApplicationManager.getApplication().isUnitTestMode()) {
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
   * Quick check: does this module have Guice on its classpath?
   * Checks if {@code com.google.inject.Inject} is resolvable.
   *
   * @param module the module to check
   * @return {@code true} if Guice is available on this module's classpath
   */
  public boolean isGuiceAvailable(@NotNull Module module) {
    return JavaLibraryUtil.hasLibraryClass(module, GuiceAnnotations.INJECT);
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
   */
  void removeFile(@NotNull VirtualFile file) {
    String path = file.getPath();
    Set<String> dependentRegistrars =
        myNavigationIndex.getDependentRegistrarFilesForPathOrPrefix(path, file.isDirectory());

    myNavigationIndex.removeFile(path);
    myFilesByPath.remove(path);
    myFileStamps.remove(path);
    myDirtyFiles.remove(file);

    if (file.isDirectory()) {
      String prefix = path + "/";
      for (String indexedPath : myNavigationIndex.getIndexedFiles()) {
        if (indexedPath.startsWith(prefix)) {
          myNavigationIndex.removeFile(indexedPath);
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
  }

  /**
   * Signals that the project structure has changed (e.g., libraries or modules
   * were added/removed). The next {@link #getNavigationIndex(Module)} call will
   * schedule a re-population of the index in the background.
   *
   * <p>Called by {@link GuiceWorkspaceModelListener} when the workspace model
   * reports changes to {@code LibraryEntity}, {@code ModuleEntity}, {@code ContentRootEntity},
   * or {@code SourceRootEntity}.
   */
  void markStructureChanged() {
    myStructureGeneration.incrementAndGet();
    myStructureChanged = true;
    myPopulationScheduled.set(false);
    if (!ApplicationManager.getApplication().isUnitTestMode() && !myProject.isDisposed()) {
      DaemonCodeAnalyzer.getInstance(myProject).restart("Guice project structure changed");
    }
  }

  // -----------------------------------------------------------------------
  // Disposable
  // -----------------------------------------------------------------------

  @Override
  public void dispose() {
    myNavigationIndex.clear();
    myFileStamps.clear();
    myDirtyFiles.clear();
    myFilesByPath.clear();
    myInitialized = false;
    myPopulationScheduled.set(false);
  }

  // -----------------------------------------------------------------------
  // Internal: helpers for GuiceBackgroundIndexUpdater
  // -----------------------------------------------------------------------

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
   * Prunes files from {@code pathsBeforePopulation} that were not discovered in {@code processedFiles},
   * and marks population complete for {@code generation}.
   */
  boolean completePopulationIfCurrent(int generation,
                                      @NotNull Set<String> pathsBeforePopulation,
                                      @NotNull Set<VirtualFile> processedFiles) {
    if (generation != myStructureGeneration.get()) {
      return false;
    }
    Set<String> keep = new HashSet<>();
    for (VirtualFile file : processedFiles) {
      keep.add(file.getPath());
    }
    for (String path : pathsBeforePopulation) {
      if (!keep.contains(path)) {
        myNavigationIndex.removeFile(path);
        myFileStamps.remove(path);
      }
    }
    if (generation == myStructureGeneration.get()) {
      myStructureChanged = false;
      myInitialized = true;
      return true;
    }
    return false;
  }

  /**
   * Allows the next {@link #getNavigationIndex} call to schedule a population again.
   * The background updater calls this when a population ends, also after a failure.
   */
  void onPopulationFinished() {
    myPopulationScheduled.set(false);
  }

  /**
   * Builds a list of fine-grained discovery steps across all Guice-enabled project modules.
   * Each step executes a single index search inside its own read action so write actions
   * can interleave without restarting completed searches from the beginning.
   */
  @NotNull List<DiscoveryStep> createDiscoverySteps(@NotNull Module module) {
    Set<Module> guiceModules = new HashSet<>();
    guiceModules.add(module);
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
   * @return {@code true} if the index was updated or the file was removed, {@code false} if the file stamp was unchanged
   */
  boolean processSingleDirtyFile(@NotNull VirtualFile file) {
    return processFileAtomically(file, myStructureGeneration.get(), true);
  }

  /**
   * Performs a two-phase atomic update for a single file:
   * <ol>
   *   <li><b>Phase 1 (Read-only, cancellable)</b>: Resolves the PSI file, checks the {@link FileStamp},
   *       and extracts {@link GuiceEntryProducer.FileExtractionResult} into local variables without mutating state.</li>
   *   <li><b>Phase 2 (In-memory commit, non-cancellable)</b>: Updates {@link #myNavigationIndex},
   *       {@link #myFileStamps}, and {@link #myDirtyFiles} together, and queues any dependent registrars.</li>
   * </ol>
   */
  private boolean processFileAtomically(@NotNull VirtualFile vf, int generation, boolean fromDirtySet) {
    // Phase 1: Pure read-only checks and PSI/UAST extraction (cancellable)
    if (!vf.isValid()) {
      removeFile(vf);
      return true;
    }
    String path = vf.getPath();
    PsiFile psiFile = PsiManager.getInstance(myProject).findFile(vf);
    if (psiFile == null) {
      removeFile(vf);
      return true;
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
        removeFile(vf);
        return true;
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

    GuiceEntryProducer.FileExtractionResult result =
        fileForExtraction instanceof PsiClassOwner
        ? GuiceEntryProducer.extractFileData(fileForExtraction)
        : GuiceEntryProducer.FileExtractionResult.EMPTY;

    // Phase 2: Atomic in-memory commit (zero PSI access, zero cancellation points)
    myFilesByPath.put(path, vf);
    for (VirtualFile targetVf : result.referencedTargetFiles()) {
      myFilesByPath.put(targetVf.getPath(), targetVf);
    }
    myNavigationIndex.updateFile(path, result);
    myFileStamps.put(path, currentStamp);
    if (fromDirtySet) {
      myDirtyFiles.remove(vf);
    }

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
    return true;
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
