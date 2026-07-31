package com.intellij.gwt.psi;

import com.intellij.gwt.facet.GwtFacet;
import com.intellij.gwt.make.GwtCompilerPaths;
import com.intellij.gwt.module.GwtModulesManager;
import com.intellij.gwt.module.model.GwtFileSet;
import com.intellij.gwt.module.model.GwtModule;
import com.intellij.gwt.module.model.GwtRelativePath;
import com.intellij.gwt.module.model.impl.GwtFilePatternUtil;
import com.intellij.gwt.superSource.GwtSuperSourceClassCache;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.components.PathMacroManager;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModuleRootEvent;
import com.intellij.openapi.roots.ModuleRootListener;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.roots.impl.JavaLanguageLevelPusher;
import com.intellij.openapi.util.JDOMUtil;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.openapi.vfs.newvfs.BulkFileListener;
import com.intellij.openapi.vfs.newvfs.events.VFileContentChangeEvent;
import com.intellij.openapi.vfs.newvfs.events.VFileCopyEvent;
import com.intellij.openapi.vfs.newvfs.events.VFileCreateEvent;
import com.intellij.openapi.vfs.newvfs.events.VFileDeleteEvent;
import com.intellij.openapi.vfs.newvfs.events.VFileEvent;
import com.intellij.openapi.vfs.newvfs.events.VFileMoveEvent;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiTreeChangeAdapter;
import com.intellij.psi.PsiTreeChangeEvent;
import com.intellij.util.ArrayUtilRt;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.messages.MessageBusConnection;
import com.intellij.util.ui.update.DebouncedUpdates;
import com.intellij.util.ui.update.UpdateQueue;
import com.intellij.util.xmlb.XmlSerializer;
import com.intellij.webcore.ModuleHelper;
import kotlinx.coroutines.CoroutineScope;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.TestOnly;
import org.jetbrains.jps.gwt.index.GwtModuleXmlConstants;
import org.jetbrains.jps.gwt.model.impl.GwtSourcePath;
import org.jetbrains.jps.gwt.model.impl.GwtSourcePathsSet;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.util.Collections.emptySet;

@Service(Service.Level.PROJECT)
public final class GwtSourcePathsRefresher implements Disposable {
  private static final Logger LOG = Logger.getInstance(GwtSourcePathsRefresher.class);

  public static GwtSourcePathsRefresher getInstance(Project project) {
    return project.getService(GwtSourcePathsRefresher.class);
  }

  private final Project myProject;
  private volatile GwtSourcePathsSet mySourcePathsSet = new GwtSourcePathsSet();
  private final UpdateQueue<Boolean> myUpdateQueue;

  public GwtSourcePathsRefresher(Project project, CoroutineScope scope) {
    myProject = project;
    int delayMillis = ApplicationManager.getApplication().isUnitTestMode() ? 0 : 1000;
    myUpdateQueue = DebouncedUpdates.<Boolean>forScope(scope, "GwtSourcePathRefresherUpdateQueue", delayMillis)
      .runBatched(batch -> refreshSourcePathsTask(ContainerUtil.exists(batch, it -> it)));

    if (!ApplicationManager.getApplication().isUnitTestMode()) {
      File gwtSourcePathsFile = GwtCompilerPaths.getGwtSourcePaths(project);
      if (gwtSourcePathsFile.exists()) {
        try {
          Element element = JDOMUtil.load(gwtSourcePathsFile);
          PathMacroManager.getInstance(myProject).expandPaths(element);
          GwtSourcePathsSet storedSet = new GwtSourcePathsSet();
          XmlSerializer.deserializeInto(storedSet, element);
          mySourcePathsSet = storedSet;
        }
        catch (JDOMException | IOException e) {
          LOG.error("Unable to load file " + gwtSourcePathsFile.getAbsolutePath(), e);
        }
      }
    }

    project.getMessageBus().connect().subscribe(ModuleRootListener.TOPIC, new ModuleRootListener() {
      @Override
      public void rootsChanged(@NotNull ModuleRootEvent event) {
        GwtLanguageLevelPusher.updateConfigurationAndPush(event.getProject(), false);
      }
    });

    init();
  }

  @TestOnly
  public boolean isAllExecuted() {
    return myUpdateQueue.isAllExecuted();
  }
  
  @Override
  public void dispose() {
    if (!ApplicationManager.getApplication().isUnitTestMode()) {
      File gwtSourcePathsFile = GwtCompilerPaths.getGwtSourcePaths(myProject);
      GwtSourcePathsSet sourcePathsSet = mySourcePathsSet;
      Element element = XmlSerializer.serialize(sourcePathsSet);
      try {
        if (!gwtSourcePathsFile.exists()) {
          if (sourcePathsSet.mySourcePaths.isEmpty() && sourcePathsSet.mySuperSourcePaths.isEmpty()) {
            return;
          }
          FileUtil.createParentDirs(gwtSourcePathsFile);
          //noinspection ResultOfMethodCallIgnored
          gwtSourcePathsFile.createNewFile();
        }
        PathMacroManager.getInstance(myProject).collapsePathsRecursively(element);
        JDOMUtil.write(element, gwtSourcePathsFile.toPath());
      }
      catch (IOException e) {
        LOG.error("Unable to save file " + gwtSourcePathsFile.getAbsolutePath(), e);
      }
    }
  }

  private void init() {
    MessageBusConnection busConnection = myProject.getMessageBus().connect();
    busConnection.subscribe(VirtualFileManager.VFS_CHANGES, new BulkFileListener() {

      @Override
      public void after(@NotNull List<? extends @NotNull VFileEvent> events) {
        processAfterVfsChanges(events);
      }
    });

    PsiManager.getInstance(myProject).addPsiTreeChangeListener(new MyPsiTreeChangeListener(), this);
  }

  public void enqueueRefreshSourcePathsTask(boolean forcePush) {
    invalidateSuperSourceCache();
    LOG.debug("Enqueue update");
    myUpdateQueue.queue(forcePush);
  }

  private void refreshSourcePathsTask(boolean forcePush) {
    LOG.debug("started processing ");
    if (myProject.isDisposed()) return;
    boolean push = ReadAction.nonBlocking(() -> {
      Set<GwtSourcePath> sourcePaths = getPaths(GwtModule::getSourceRoots);
      Set<GwtSourcePath> superSourcePaths = getPaths(GwtModule::getSuperSourceRoots);

      GwtSourcePathsSet currentSet = mySourcePathsSet;
      if (sourcePaths.equals(currentSet.mySourcePaths) && superSourcePaths.equals(currentSet.mySuperSourcePaths)) {
        return false;
      }
      GwtSourcePathsSet newSet = new GwtSourcePathsSet();
      newSet.mySourcePaths.addAll(sourcePaths);
      newSet.mySuperSourcePaths.addAll(superSourcePaths);
      mySourcePathsSet = newSet;
      return true;
    })
      .inSmartMode(myProject)
      .expireWith(GwtSourcePathsRefresher.this)
      .executeSynchronously();

    if (push || forcePush) {
      JavaLanguageLevelPusher.pushLanguageLevel(myProject);
    }
    if (LOG.isDebugEnabled()) {
      LOG.debug("finished processing ");
      LOG.debug("source paths = " + mySourcePathsSet.mySourcePaths.stream().map(GwtSourcePath::getFullPath).collect(Collectors.joining(", ")));
      LOG.debug("super source paths = " + mySourcePathsSet.mySuperSourcePaths.stream().map(GwtSourcePath::getFullPath).collect(Collectors.joining(", ")));
    }
  }


  private void invalidateSuperSourceCache() {
    GwtSuperSourceClassCache.getInstance(myProject).invalidateSuperSourceCache();
  }

  private @NotNull Set<GwtSourcePath> getPaths(Function<GwtModule, Map<VirtualFile, GwtRelativePath>> getPaths) {
    GwtModulesManager gwtModulesManager = GwtModulesManager.getInstance(myProject);
    Set<GwtSourcePath> sourcePaths = new HashSet<>();
    for (Module module : ModuleHelper.getModules(myProject)) {
      ProgressManager.checkCanceled();

      GwtFacet gwtFacet = GwtFacet.getInstance(module);
      if (gwtFacet == null) continue;

      List<GwtModule> gwtModules = gwtModulesManager.getGwtModules(module, true);
      for (GwtModule gwtModule : gwtModules) {
        ProgressManager.checkCanceled();

        Map<VirtualFile, GwtRelativePath> sourceRoots = getPaths.apply(gwtModule);
        for (Map.Entry<VirtualFile, GwtRelativePath> entry : sourceRoots.entrySet()) {
          ProgressManager.checkCanceled();

          GwtRelativePath relativePath = entry.getValue();
          GwtSourcePath gwtSourcePath;
          if (relativePath != null) {
            gwtSourcePath = new GwtSourcePath(
              entry.getKey().getPath(),
              getBoolean(relativePath.isCaseSensitive().getValue()),
              getBoolean(relativePath.isDefaultExcludes()),
              mergePatterns(relativePath.getIncludesAttribute().getValue(), relativePath.getIncludes()),
              mergePatterns(relativePath.getExcludesAttribute().getValue(), relativePath.getExcludes())
            );
          }
          else {
            gwtSourcePath = new GwtSourcePath(entry.getKey().getPath(), true, true, Collections.emptyList(), Collections.emptyList());
          }
          sourcePaths.add(gwtSourcePath);
        }
      }
    }
    return sourcePaths;
  }

  public boolean isSourceFile(VirtualFile file) {
    if (ProjectRootManager.getInstance(myProject).getFileIndex().isInTestSourceContent(file)) return false;
    return isInPaths(file, mySourcePathsSet.mySourcePaths);
  }

  public boolean isSuperSourceFile(VirtualFile file) {
    return isInPaths(file, mySourcePathsSet.mySuperSourcePaths);
  }

  public Set<GwtSourcePath> getSuperSourcePaths(List<GwtModule> gwtModules) {
    if (gwtModules.isEmpty()) return emptySet();

    List<String> gwtModulePaths = new ArrayList<>(gwtModules.size());
    for (GwtModule gwtModule : gwtModules) {
      gwtModulePaths.add(gwtModule.getModuleDirectory().getPath());
    }

    Set<GwtSourcePath> superSourcePaths = new HashSet<>();
    for (GwtSourcePath gwtSourcePath : mySourcePathsSet.mySuperSourcePaths) {
      for (String modulePath : gwtModulePaths) {
        if (FileUtil.isAncestor(modulePath, gwtSourcePath.getFullPath(), false)) {
          superSourcePaths.add(gwtSourcePath);
          break;
        }
      }
    }

    return superSourcePaths;
  }

  private boolean isInPaths(VirtualFile file, Set<GwtSourcePath> paths) {
    for (GwtSourcePath sourcePath : paths) {
      if (FileUtil.isAncestor(sourcePath.getFullPath(), file.getPath(), false)) {
        if (isIncluded(file.getPath(), sourcePath)) return true;
      }
    }
    return false;
  }

  public boolean isIncluded(String filePath, GwtSourcePath sourcePath) {
    String path = FileUtil.getRelativePath(sourcePath.getFullPath(), filePath, '/');
    if (path == null) return false;

    if (sourcePath.defaultExcludes() && GwtFilePatternUtil.isExcludedByDefault(path, sourcePath.caseSensitive())) {
      return false;
    }

    return !sourcePath.excludes(path) && sourcePath.includes(path);
  }

  private void processAfterVfsChanges(@NotNull List<? extends VFileEvent> events) {
    for (VFileEvent event : events) {
      VirtualFile file = event.getFile();
      if (file == null) {
        continue;
      }
      if (file.isDirectory()) {
        if (event instanceof VFileMoveEvent ||
            event instanceof VFileCopyEvent ||
            event instanceof VFileDeleteEvent) {
          enqueueRefreshSourcePathsTask(false);
          return;
        }
      }
      else {
        invalidateSuperSourceCache();
        if (!file.getName().endsWith(GwtModuleXmlConstants.GWT_XML_SUFFIX)) {
          continue;
        }

        if (event instanceof VFileCreateEvent ||
            event instanceof VFileMoveEvent   ||
            event instanceof VFileDeleteEvent ||
            event instanceof VFileContentChangeEvent) {
          enqueueRefreshSourcePathsTask(false);
          return;
        }
      }
    }
  }

  private static boolean getBoolean(@Nullable Boolean value) {
    return value == null || value;
  }

  private static @NotNull List<String> mergePatterns(@Nullable String patterns, @Nullable List<GwtFileSet> patternsList) {
    if (patterns == null && ContainerUtil.isEmpty(patternsList)) return ContainerUtil.emptyList();

    String[] patternsArray = patterns == null ? ArrayUtilRt.EMPTY_STRING_ARRAY : patterns.split("(\\s|,)");
    patternsList = ContainerUtil.notNullize(patternsList);

    List<String> result = new ArrayList<>(patternsArray.length + patternsList.size());

    Collections.addAll(result, patternsArray);
    for (GwtFileSet fileSet : patternsList) {
      ContainerUtil.addIfNotNull(result, fileSet.getName().getValue());
    }

    return result;
  }

  private class MyPsiTreeChangeListener extends PsiTreeChangeAdapter {

    private void processEvent(@NotNull PsiTreeChangeEvent event) {
      PsiFile file = event.getFile();
      if (file == null || !file.getName().endsWith(GwtModuleXmlConstants.GWT_XML_SUFFIX)) {
        return;
      }

      enqueueRefreshSourcePathsTask(false);
    }

    @Override
    public void childAdded(@NotNull PsiTreeChangeEvent event) {
      processEvent(event);
    }

    @Override
    public void childRemoved(@NotNull PsiTreeChangeEvent event) {
      processEvent(event);
    }

    @Override
    public void childReplaced(@NotNull PsiTreeChangeEvent event) {
      processEvent(event);
    }

    @Override
    public void childrenChanged(@NotNull PsiTreeChangeEvent event) {
      processEvent(event);
    }

    @Override
    public void childMoved(@NotNull PsiTreeChangeEvent event) {
      processEvent(event);
    }
  }
}
