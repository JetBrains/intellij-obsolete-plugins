package com.intellij.gwt.run;

import com.intellij.execution.ExecutionManager;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.gwt.GwtBundle;
import com.intellij.gwt.module.GwtModulesManager;
import com.intellij.gwt.module.model.GwtModule;
import com.intellij.ide.highlighter.JavaFileType;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.fileTypes.FileTypeRegistry;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.DumbModeBlockedFunctionality;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.IndexNotReadyException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.NlsContexts;
import com.intellij.openapi.vfs.StandardFileSystems;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.pom.Navigatable;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiFile;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.util.Url;
import com.intellij.util.Urls;
import com.jetbrains.javascript.debugger.FileUrlMapper;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntMaps;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.debugger.sourcemap.SourceFileResolver;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

final class GwtFileUrlMapper extends FileUrlMapper {
  private static final int DEFAULT_PORT_NUMBER = 9876;
  private static final Key<Integer> PORT_KEY = Key.create("gwtCodeServerPort");
  private static final @NonNls String PATH_PREFIX = "/sourcemaps/";

  @Override
  public @NotNull List<Url> getUrls(@NotNull VirtualFile file, @NotNull Project project, @Nullable String currentAuthority) {
    if (!FileTypeRegistry.getInstance().isFileOfType(file, JavaFileType.INSTANCE)) {
      return Collections.emptyList();
    }

    Set<String> ports = new HashSet<>();
    ports.add(String.valueOf(DEFAULT_PORT_NUMBER));
    for (ProcessHandler processHandler : ExecutionManager.getInstance(project).getRunningProcesses()) {
      Integer port = PORT_KEY.get(processHandler);
      if (port != null) {
        ports.add(String.valueOf(port));
      }
    }

    return doCreateUrl(file, project, ports);
  }

  private static @NotNull List<Url> doCreateUrl(final @NotNull VirtualFile file, final @NotNull Project project, final Collection<String> ports) {
    List<Url> urls = nonBlockingReadActionInSmartMode(project, () -> {
      GwtModulesManager gwtModulesManager = GwtModulesManager.getInstance(project);
      VirtualFile sourceRoot = ProjectRootManager.getInstance(project).getFileIndex().getSourceRootForFile(file);
      if (sourceRoot == null || sourceRoot.getFileSystem() == StandardFileSystems.jar()) {
        return Collections.emptyList();
      }

      List<GwtModule> gwtModules = gwtModulesManager.findGwtModulesByClientSourceFile(file);
      if (gwtModules.isEmpty()) {
        return Collections.emptyList();
      }

      String relativeUrlPath = VfsUtilCore.getRelativePath(file, sourceRoot, '/');
      if (relativeUrlPath == null) {
        return Collections.emptyList();
      }

      Url[] result = new Url[gwtModules.size() * ports.size()];
      for (int i = 0; i < gwtModules.size(); i++) {
        String path = PATH_PREFIX + gwtModules.get(i).getOutputName() + '/' + relativeUrlPath;
        for (String port : ports) {
          result[i] = Urls.newHttpUrl("localhost:" + port, path);
        }
      }
      return Arrays.asList(result);
    }, GwtBundle.message("notification.content.gwt.file.mapping.is.not.possible.during.index.update"));
    return urls == null ? Collections.emptyList() : urls;
  }

  @Override
  public @Nullable SourceFileResolver createSourceResolver(final @NotNull VirtualFile file, final @NotNull Project project) {
    if (!FileTypeRegistry.getInstance().isFileOfType(file, JavaFileType.INSTANCE)) {
      return null;
    }

    return nonBlockingReadActionInSmartMode(project, (Supplier<SourceFileResolver>)() -> {
      VirtualFile sourceRoot = ProjectRootManager.getInstance(project).getFileIndex().getSourceRootForFile(file);
      if (sourceRoot == null) {
        return null;
      }

      GwtModulesManager gwtModulesManager = GwtModulesManager.getInstance(project);
      /* gwt code server run on module A.
      A inherits module B.
      Our current file belongs to module B.

      So, actual:
      http://localhost:9876/sourcemaps/A/com/jetbrains/upsource/frontend/application/client/views/feedView/FeedView.java

      expected:
      http://localhost:9876/sourcemaps/B/com/jetbrains/upsource/frontend/application/client/views/feedView/FeedView.java

      So, we must handle this case, not only library classes
      */
      String relativePath = gwtModulesManager.isUnderGwtModule(file) ? VfsUtilCore.getRelativePath(file, sourceRoot, '/') : null;
      return relativePath == null ? null : new MySourceResolver(relativePath);
    }, GwtBundle.message("notification.content.gwt.file.mapping.is.not.possible.during.index.update"));
  }

  private static @Nullable PsiClass findClass(@NotNull Url url, final @NotNull Project project) {
    final String path = url.getPath();
    if (!(path.startsWith(PATH_PREFIX) && path.endsWith(JavaFileType.DOT_DEFAULT_EXTENSION))) {
      return null;
    }

    int index = path.indexOf('/', PATH_PREFIX.length() + 1);
    if (index < 0 || path.length() < (index + 2 + JavaFileType.DOT_DEFAULT_EXTENSION.length())) {
      return null;
    }

    final String moduleName = path.substring(PATH_PREFIX.length(), index);
    final String className = path.substring(index + 1, path.length() - JavaFileType.DOT_DEFAULT_EXTENSION.length()).replace('/', '.');

    return nonBlockingReadActionInSmartMode(project, () -> {
      GwtModulesManager gwtModulesManager = GwtModulesManager.getInstance(project);
      for (GwtModule gwtModule : gwtModulesManager.findGwtModulesByOutputName(moduleName, GlobalSearchScope.projectScope(project))) {
        Module module = gwtModule.getModule();
        if (module != null) {
          PsiClass[] psiClasses = JavaPsiFacade.getInstance(project).findClasses(className, module.getModuleWithLibrariesScope());
          if (psiClasses.length == 1) {
            return psiClasses[0];
          }
          else if (psiClasses.length > 1) {
            for (PsiClass psiClass : psiClasses) {
              if (psiClass.canNavigateToSource() && psiClass.getNavigationElement().getContainingFile().getFileType() == JavaFileType.INSTANCE) {
                return psiClass;
              }
            }
            Arrays.sort(psiClasses, new Comparator<>() {
              private int getWeight(@NotNull PsiClass psiClass) {
                if (psiClass.canNavigateToSource()) {
                  return 2;
                }
                return psiClass.canNavigate() ? 1 : 0;
              }

              @Override
              public int compare(@NotNull PsiClass c1, @NotNull PsiClass c2) {
                return getWeight(c1) - getWeight(c2);
              }
            });
            return psiClasses[psiClasses.length - 1];
          }
        }
      }
      return null;
    }, GwtBundle.message("notification.content.gwt.file.mapping.is.not.possible.during.index.update"));
  }

  private static <T> T nonBlockingReadActionInSmartMode(@NotNull Project project,
                                                        @NotNull Supplier<? extends T> task,
                                                        @NotNull @NlsContexts.PopupContent String notification) {
    if (ApplicationManager.getApplication().isReadAccessAllowed()) {
      try {
        return task.get();
      }
      catch (IndexNotReadyException e) {
        DumbService.getInstance(project).showDumbModeNotificationForFunctionality(notification, DumbModeBlockedFunctionality.Gwt);
        return null;
      }
    }

    return ReadAction.nonBlocking(() -> task.get())
      .inSmartMode(project)
      .executeSynchronously();
  }

  @Override
  public @Nullable Navigatable getNavigatable(@NotNull Url url, @NotNull Project project, @Nullable Url requestor) {
    return findClass(url, project);
  }

  @Override
  public @Nullable VirtualFile getFile(@NotNull Url url, final @NotNull Project project, @Nullable Url requestor) {
    PsiClass psiClass = findClass(url, project);
    if (psiClass == null) {
      return null;
    }
    else {
      return ReadAction.computeBlocking(()-> {
        PsiFile psiFile;
        psiFile = psiClass.getNavigationElement().getContainingFile();
        if (psiFile == null) {
          psiFile = psiClass.getContainingFile();
        }
        return psiFile == null ? null : psiFile.getVirtualFile();
      });
    }
  }

  private static final class MySourceResolver implements SourceFileResolver {
    private final String relativeUrlPath;

    MySourceResolver(@NotNull String relativeUrlPath) {
      this.relativeUrlPath = relativeUrlPath;
    }

    @Override
    public int resolve(@NotNull Object2IntMap<Url> map) {
      final AtomicInteger result = new AtomicInteger(-1);
      for (Object2IntMap.Entry<Url> entry : Object2IntMaps.fastIterable(map)) {
        String path = entry.getKey().getPath();
        int pathLength = path.length();
        int postfixLength = relativeUrlPath.length();
        if (pathLength > postfixLength && path.charAt(pathLength - postfixLength - 1) == '/' && path.endsWith(relativeUrlPath)) {
          result.set(entry.getIntValue());
          break;
        }
      }
      return result.get();
    }

    @Override
    public int resolve(@NotNull List<String> rawSources) {
      return -1;
    }
  }
}
