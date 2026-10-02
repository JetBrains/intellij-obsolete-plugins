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
 *   <li><b>Single index</b>: A {@link GuiceNavigationIndex} is created once and never rebuilt
 *       from scratch (unless project structure changes).  Each file's {@link GuiceEntry} set is
 *       extracted independently by {@link GuiceEntryProducer} and stored per-file.</li>
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
 *       {@link GuiceNavigationIndex} is thread-safe.
 *       Concurrent writes from the highlighting thread and background thread are safe.</li>
 * </ul>
 *
 * @see GuiceNavigationIndex
 * @see GuiceVfsListener
 */
@Service(Service.Level.PROJECT)
public final class GuiceProjectModel implements Disposable {

  private final Project myProject;

  /**
   * The unified navigation index — provides symmetric navigation guarantees.
   * Populated per file during file processing.
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
   * Set to {@code true} when the project structure changes (modules, libraries,
   * or content/source roots added or removed). This triggers a full re-population
   * scheduled via {@link #myBackgroundUpdater}.
   */
  private volatile boolean myStructureChanged = true;

  /**
   * Counts structure changes. A population records the value when it starts.
   * If the value changed while the population ran, the population does not clear {@link #myStructureChanged}.
   */
  private final AtomicInteger myStructureGeneration = new AtomicInteger();

  /**
   * Whether the initial population has been performed.  Set to {@code true} after
   * {@link #markPopulationComplete(int)} is called by the background updater.
   */
  private volatile boolean myInitialized = false;

  /**
   * Guards initial population: set to true when a background population has
   * been scheduled, to prevent scheduling duplicates.
   */
  private final AtomicBoolean myPopulationScheduled = new AtomicBoolean();

  /**
   * The PSI modification stamp of each file at its last inline re-index.
   * The line marker pass asks for the same file more than once per pass. The stamp lets it skip the repeated work.
   */
  private final ConcurrentHashMap<String, Long> myInlineStamps = new ConcurrentHashMap<>();

  /**
   * The PSI modification stamp of each external target file
   * at the last time its dependent registrar files were indexed.
   */
  private final ConcurrentHashMap<String, Long> myTargetStamps = new ConcurrentHashMap<>();

  /**
   * Maps file paths to their {@link VirtualFile} instances for fast lookup of dependent registrar
   * and target files across both local and in-memory file systems.
   */
  private final ConcurrentHashMap<String, VirtualFile> myFilesByPath = new ConcurrentHashMap<>();

  /**
   * Per-file VFS modification stamps.  Used to detect whether a file in the dirty
   * set has actually changed (VFS may fire spurious events).
   */
  private final ConcurrentHashMap<VirtualFile, Long> myFileStamps = new ConcurrentHashMap<>();

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
    if ((!myInitialized || myStructureChanged) && myPopulationScheduled.compareAndSet(false, true)) {
      myBackgroundUpdater.scheduleInitialPopulation(module, myStructureGeneration.get());
    }
    if (ApplicationManager.getApplication().isUnitTestMode()) {
      myBackgroundUpdater.processDirtyFilesNow();
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
    refreshTrackedTargetsInline();
    if (!isIndexableEditorFile(file)) return;
    VirtualFile vf = file.getVirtualFile();
    String path = vf.getPath();
    myFilesByPath.put(path, vf);
    long stamp = file.getModificationStamp();
    Long previous = myInlineStamps.get(path);
    if (previous != null && previous == stamp) return;

    GuiceEntryProducer.FileExtractionResult result = GuiceEntryProducer.extractFileData(file);
    myNavigationIndex.updateFile(path, result);
    recordReferencedTargets(result);
    myInlineStamps.put(path, stamp);
    myTargetStamps.put(path, stamp);

    Set<String> dependentRegistrars = new LinkedHashSet<>(myNavigationIndex.getDependentRegistrarFiles(path));
    dependentRegistrars.addAll(myNavigationIndex.getDependentRegistrarFilesByClassName(vf.getNameWithoutExtension()));
    dependentRegistrars.remove(path);
    reindexRegistrarFilesInline(dependentRegistrars);
  }

  private void refreshTrackedTargetsInline() {
    Set<String> trackedTargets = myNavigationIndex.getTrackedTargetPaths();
    if (trackedTargets.isEmpty()) return;

    Set<String> registrarsToReindex = new LinkedHashSet<>();
    PsiManager psiManager = PsiManager.getInstance(myProject);
    for (String targetPath : trackedTargets) {
      VirtualFile targetVf = resolveVirtualFile(targetPath);
      if (targetVf == null || !targetVf.isValid()) {
        myTargetStamps.remove(targetPath);
        registrarsToReindex.addAll(myNavigationIndex.getDependentRegistrarFiles(targetPath));
        continue;
      }
      PsiFile targetPsi = psiManager.findFile(targetVf);
      if (targetPsi == null) {
        myTargetStamps.remove(targetPath);
        registrarsToReindex.addAll(myNavigationIndex.getDependentRegistrarFiles(targetPath));
        continue;
      }
      long currentStamp = targetPsi.getModificationStamp();
      Long recordedStamp = myTargetStamps.get(targetPath);
      if (recordedStamp == null || recordedStamp != currentStamp) {
        myTargetStamps.put(targetPath, currentStamp);
        registrarsToReindex.addAll(myNavigationIndex.getDependentRegistrarFiles(targetPath));
      }
    }
    if (!registrarsToReindex.isEmpty()) {
      reindexRegistrarFilesInline(registrarsToReindex);
    }
  }

  private void reindexRegistrarFilesInline(@NotNull Set<String> registrarPaths) {
    if (registrarPaths.isEmpty()) return;
    PsiManager psiManager = PsiManager.getInstance(myProject);
    for (String registrarPath : registrarPaths) {
      VirtualFile registrarVf = resolveVirtualFile(registrarPath);
      if (registrarVf == null || !registrarVf.isValid()) continue;
      PsiFile registrarPsi = psiManager.findFile(registrarVf);
      if (registrarPsi == null) continue;
      GuiceEntryProducer.FileExtractionResult result = GuiceEntryProducer.extractFileData(registrarPsi);
      myNavigationIndex.updateFile(registrarPath, result);
      recordReferencedTargets(result);
      myInlineStamps.put(registrarPath, registrarPsi.getModificationStamp());
    }
  }

  private void recordReferencedTargets(@NotNull GuiceEntryProducer.FileExtractionResult result) {
    PsiManager psiManager = PsiManager.getInstance(myProject);
    for (VirtualFile targetVf : result.referencedTargetFiles()) {
      String targetPath = targetVf.getPath();
      myFilesByPath.put(targetPath, targetVf);
      PsiFile targetPsi = psiManager.findFile(targetVf);
      if (targetPsi != null) {
        myTargetStamps.put(targetPath, targetPsi.getModificationStamp());
      }
    }
  }

  private @Nullable VirtualFile resolveVirtualFile(@NotNull String path) {
    VirtualFile vf = myFilesByPath.get(path);
    if (vf != null) return vf;
    return LocalFileSystem.getInstance().findFileByPath(path);
  }

  /**
   * Tells if the inline re-index may write the entries of the file into the index.
   * A diff or merge copy, a quick-fix preview, or a light file would overwrite the entries of the real file,
   * because the index uses the file path as the key.
   */
  private boolean isIndexableEditorFile(@NotNull PsiFile file) {
    VirtualFile vf = file.getVirtualFile();
    return vf != null &&
           !(vf instanceof LightVirtualFile) &&
           file.isPhysical() &&
           file.getViewProvider().isEventSystemEnabled() &&
           file.getOriginalFile() == file &&
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
    myInlineStamps.remove(path);
    myTargetStamps.remove(path);
    myFilesByPath.remove(path);
    myFileStamps.remove(file);
    myDirtyFiles.remove(file);

    if (file.isDirectory()) {
      String prefix = path + "/";
      for (String indexedPath : myNavigationIndex.getIndexedFiles()) {
        if (indexedPath.startsWith(prefix)) {
          myNavigationIndex.removeFile(indexedPath);
          myInlineStamps.remove(indexedPath);
        }
      }
      myTargetStamps.keySet().removeIf(p -> p.startsWith(prefix));
      myFilesByPath.keySet().removeIf(p -> p.startsWith(prefix));
      myFileStamps.keySet().removeIf(vf -> vf.getPath().startsWith(prefix));
      myDirtyFiles.removeIf(vf -> vf.getPath().startsWith(prefix));
    }

    for (String registrarPath : dependentRegistrars) {
      if (registrarPath.equals(path) || (file.isDirectory() && registrarPath.startsWith(path + "/"))) {
        continue;
      }
      myInlineStamps.remove(registrarPath);
      VirtualFile registrarVf = resolveVirtualFile(registrarPath);
      if (registrarVf != null && registrarVf.isValid()) {
        myFileStamps.remove(registrarVf);
        markFileDirty(registrarVf);
      }
    }
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
    myStructureGeneration.incrementAndGet();
    myStructureChanged = true;
    myInlineStamps.clear();
    myTargetStamps.clear();
    myPopulationScheduled.set(false);  // Allow re-scheduling of background population.
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
    myInlineStamps.clear();
    myTargetStamps.clear();
    myFilesByPath.clear();
    myInitialized = false;
    myPopulationScheduled.set(false);
  }

  // -----------------------------------------------------------------------
  // Internal: helpers for GuiceBackgroundIndexUpdater
  // -----------------------------------------------------------------------

  @NotNull Set<String> getIndexedFilesSnapshot() {
    return myNavigationIndex.getIndexedFiles();
  }

  /**
   * Removes the entries of files from {@code pathsBeforePopulation} that the last population did not find.
   * Files added concurrently while the population ran are not in {@code pathsBeforePopulation} and are kept.
   *
   * @param pathsBeforePopulation the file paths present in the index when the population started
   * @param processedFiles        the files that the population processed
   */
  void pruneFilesExcept(@NotNull Set<String> pathsBeforePopulation, @NotNull Set<VirtualFile> processedFiles) {
    Set<String> keep = new HashSet<>();
    for (VirtualFile file : processedFiles) {
      keep.add(file.getPath());
    }
    for (String path : pathsBeforePopulation) {
      if (!keep.contains(path)) {
        myNavigationIndex.removeFile(path);
        myInlineStamps.remove(path);
      }
    }
    myFileStamps.keySet().removeIf(file -> pathsBeforePopulation.contains(file.getPath()) && !keep.contains(file.getPath()));
  }

  /**
   * Marks the index as fully populated.
   * Clears the structure-changed flag only if no structure change came while the population ran.
   * Called by the background updater after all files have been processed.
   *
   * @param generation the value of the structure generation when the population started
   */
  void markPopulationComplete(int generation) {
    if (generation == myStructureGeneration.get()) {
      myStructureChanged = false;
    }
    myInitialized = true;
  }

  /**
   * Allows the next {@link #getNavigationIndex} call to schedule a population again.
   * The background updater calls this when a population ends, also after a failure.
   */
  void onPopulationFinished() {
    myPopulationScheduled.set(false);
  }

  /**
   * Discovers all files across all Guice-enabled project modules that contain
   * Guice-relevant annotations or Guice module classes.
   *
   * @param module the IntelliJ module that triggered population
   * @return a set of virtual files that should be processed for Guice data
   */
  @NotNull Set<VirtualFile> discoverRelevantFiles(@NotNull Module module) {
    Set<VirtualFile> files = new HashSet<>();
    Set<Module> guiceModules = new HashSet<>();
    guiceModules.add(module);
    for (Module m : ModuleManager.getInstance(myProject).getModules()) {
      if (isGuiceAvailable(m)) {
        guiceModules.add(m);
      }
    }
    GlobalSearchScope scope = GlobalSearchScope.EMPTY_SCOPE;
    for (Module m : guiceModules) {
      scope = scope.union(GlobalSearchScope.moduleWithDependenciesAndLibrariesScope(m));
    }
    JavaPsiFacade facade = JavaPsiFacade.getInstance(myProject);
    GuiceExtensionIndex extensionIndex = GuiceExtensionIndex.get();

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

    // Files with contributor-registered field annotations
    for (String fieldAnno : extensionIndex.getFieldAnnotations()) {
      PsiClass annoClass = facade.findClass(fieldAnno, GlobalSearchScope.allScope(myProject));
      if (annoClass == null) continue;
      for (PsiField field : AnnotatedElementsSearch.searchPsiFields(annoClass, scope).findAll()) {
        addFileOf(field, files);
      }
    }

    // Files with @Provides and contributor-registered method annotations
    for (String providesAnno : extensionIndex.getAllProvidesAnnotations()) {
      PsiClass annoClass = facade.findClass(providesAnno, GlobalSearchScope.allScope(myProject));
      if (annoClass == null) continue;
      for (PsiMethod method : AnnotatedElementsSearch.searchPsiMethods(annoClass, scope).findAll()) {
        addFileOf(method, files);
      }
    }
    for (String methodAnno : extensionIndex.getMethodAnnotations()) {
      PsiClass annoClass = facade.findClass(methodAnno, GlobalSearchScope.allScope(myProject));
      if (annoClass == null) continue;
      for (PsiMethod method : AnnotatedElementsSearch.searchPsiMethods(annoClass, scope).findAll()) {
        addFileOf(method, files);
      }
    }

    // Classes with @ImplementedBy, @ProvidedBy, or contributor-registered class annotations
    for (String jitAnno : List.of(GuiceAnnotations.IMPLEMENTED_BY, GuiceAnnotations.PROVIDED_BY)) {
      PsiClass annoClass = facade.findClass(jitAnno, GlobalSearchScope.allScope(myProject));
      if (annoClass == null) continue;
      for (PsiClass cls : AnnotatedElementsSearch.searchPsiClasses(annoClass, scope).findAll()) {
        addFileOf(cls, files);
      }
    }
    for (String classAnno : extensionIndex.getClassAnnotations()) {
      PsiClass annoClass = facade.findClass(classAnno, GlobalSearchScope.allScope(myProject));
      if (annoClass == null) continue;
      for (PsiClass cls : AnnotatedElementsSearch.searchPsiClasses(annoClass, scope).findAll()) {
        addFileOf(cls, files);
      }
    }

    // Guice module files (for bindings defined in configure())
    for (PsiClass cls : GuiceInjectorManager.getGuiceModuleClasses(module, scope)) {
      addFileOf(cls, files);
    }

    // Files calling contributor-registered global binder helpers outside Module classes
    for (String ownerFqn : extensionIndex.getGlobalCallOwnerClasses()) {
      PsiClass ownerClass = facade.findClass(ownerFqn, GlobalSearchScope.allScope(myProject));
      if (ownerClass == null) continue;
      for (PsiReference ref : ReferencesSearch.search(ownerClass, scope).findAll()) {
        addFileOf(ref.getElement(), files);
      }
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
  private void addFileOf(@NotNull PsiElement element, @NotNull Set<VirtualFile> files) {
    PsiFile file = element.getContainingFile();
    if (file == null) return;

    // For compiled classes, prefer the source file if available.
    if (file instanceof PsiCompiledFile clsFile) {
      PsiElement sourceNav = clsFile.getNavigationElement();
      if (sourceNav instanceof PsiFile sourceFile && sourceFile != clsFile) {
        VirtualFile sourceVf = sourceFile.getVirtualFile();
        if (sourceVf != null) {
          myFilesByPath.put(sourceVf.getPath(), sourceVf);
          files.add(sourceVf);
          return;
        }
      }
    }

    VirtualFile vf = file.getVirtualFile();
    if (vf != null) {
      myFilesByPath.put(vf.getPath(), vf);
      files.add(vf);
    }
  }

  // -----------------------------------------------------------------------
  // Internal: per-file processing
  // -----------------------------------------------------------------------

  /**
   * Extracts all Guice data (bindings, injection points, {@code @Provides} methods)
   * from a single file and updates the live index surgically.
   *
   * @param vf the virtual file to process
   */
  void processFile(@NotNull VirtualFile vf) {
    extractAndStoreFile(vf);

    String path = vf.getPath();
    Set<String> dependentRegistrars = new LinkedHashSet<>(myNavigationIndex.getDependentRegistrarFiles(path));
    dependentRegistrars.addAll(myNavigationIndex.getDependentRegistrarFilesByClassName(vf.getNameWithoutExtension()));
    dependentRegistrars.remove(path);
    for (String registrarPath : dependentRegistrars) {
      VirtualFile registrarVf = resolveVirtualFile(registrarPath);
      if (registrarVf != null && registrarVf.isValid()) {
        extractAndStoreFile(registrarVf);
      }
    }
  }

  private void extractAndStoreFile(@NotNull VirtualFile vf) {
    String path = vf.getPath();
    myFilesByPath.put(path, vf);
    PsiFile psiFile = PsiManager.getInstance(myProject).findFile(vf);
    if (psiFile == null) {
      myNavigationIndex.removeFile(path);
      myInlineStamps.remove(path);
      myTargetStamps.remove(path);
      myFileStamps.remove(vf);
      return;
    }

    // For compiled class files, try to resolve to source for full extraction.
    // Java and Kotlin library classes both have the JAVA_CLASS file type, whichever decompiler builds the PSI.
    PsiFile fileForExtraction = psiFile;
    boolean isCompiled = psiFile instanceof PsiCompiledElement
                         || FileTypeRegistry.getInstance().isFileOfType(vf, JavaClassFileType.INSTANCE);
    if (isCompiled) {
      PsiElement sourceNav = psiFile.getNavigationElement();
      if (sourceNav instanceof PsiFile sourceFile && sourceFile != psiFile) {
        fileForExtraction = sourceFile;
      } else {
        if (!ProjectFileIndex.getInstance(myProject).isInProject(vf)) {
          // This is compiled class that is not directly owned by our project. Skip!
          myNavigationIndex.removeFile(path);
          myInlineStamps.remove(path);
          myTargetStamps.remove(path);
          myFileStamps.remove(vf);
          return;
        }
      }
    }

    // Extract navigation entries and cross-file target dependencies.
    GuiceEntryProducer.FileExtractionResult result =
        fileForExtraction instanceof PsiClassOwner
        ? GuiceEntryProducer.extractFileData(fileForExtraction)
        : GuiceEntryProducer.FileExtractionResult.EMPTY;
    myNavigationIndex.updateFile(path, result);
    recordReferencedTargets(result);

    myFileStamps.put(vf, vf.getModificationStamp());
    myTargetStamps.put(path, fileForExtraction.getModificationStamp());
  }

  /**
   * Returns a snapshot of the current dirty files without mutating state.
   *
   * <p>Validity and modification-stamp checks happen inside {@link #processSingleDirtyFile}
   * so that a cancelled read action does not drop dirty files before they are processed.
   *
   * @return the list of dirty files to inspect, never {@code null}
   */
  @NotNull List<VirtualFile> collectDirtyFiles() {
    if (myDirtyFiles.isEmpty()) return List.of();
    return new ArrayList<>(myDirtyFiles);
  }

  /**
   * Processes a single dirty file and removes it from the dirty set once extraction completes.
   *
   * <p>Must be called under a read action.
   *
   * @param file the file to process
   */
  void processSingleDirtyFile(@NotNull VirtualFile file) {
    if (!file.isValid()) {
      removeFile(file);
      return;
    }
    Long oldStamp = myFileStamps.get(file);
    long currentStamp = file.getModificationStamp();
    if (oldStamp != null && oldStamp == currentStamp) {
      myDirtyFiles.remove(file);
      return;
    }
    processFile(file);
    myDirtyFiles.remove(file);
  }
}
