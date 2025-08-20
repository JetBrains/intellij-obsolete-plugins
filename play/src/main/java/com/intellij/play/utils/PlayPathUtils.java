package com.intellij.play.utils;

import com.intellij.codeInsight.AnnotationUtil;
import com.intellij.jam.JamStringAttributeElement;
import com.intellij.jam.reflect.JamAttributeMeta;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.util.io.FileUtilRt;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.play.completion.beans.PlayFastTagDescriptor;
import com.intellij.play.constants.PlayConstants;
import com.intellij.play.language.psi.PlayPsiFile;
import com.intellij.psi.*;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.LocalSearchScope;
import com.intellij.psi.search.searches.ClassInheritorsSearch;
import com.intellij.util.Query;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public final class PlayPathUtils {

  private static final String VIEWS_PKG = "views";
  private static final String VIEWS_DEFAULT_EXTENSION = "html";


  @Nullable
  public static PsiDirectory getCorrespondingDirectory(@NotNull PsiClass controller) {
    if (PlayUtils.isController(controller)) {
      final Module module = ModuleUtilCore.findModuleForPsiElement(controller);
      if (module != null) {
        final PsiDirectory directory = getViewsDirectory(module);
        if (directory != null) {
          return directory.findSubdirectory(controller.getName());
        }
      }
    }
    return null;
  }


  @Nullable
  public static PsiClass getCorrespondingController(@NotNull PsiFile psiFile) {
    PsiDirectory psiDirectory = psiFile.getOriginalFile().getContainingDirectory();
    String path = "";
    while (psiDirectory != null && !VIEWS_PKG.equals(psiDirectory.getName())) {
      if (path.length() > 0) path += ".";
      path = psiDirectory.getName() + path;
      psiDirectory = psiDirectory.getParentDirectory();
    }
    return StringUtil.isEmptyOrSpaces(path)
           ? null
           : JavaPsiFacade.getInstance(psiFile.getProject()).findClass(PlayUtils.CONTROLLERS_PKG + "." + path, psiFile.getResolveScope());
  }

  @Nullable
  public static PsiClass findControllerByName(@NotNull String controller, @NotNull Module module) {
    if (StringUtil.isEmptyOrSpaces(controller)) return null;
    String className = controller.startsWith(PlayUtils.CONTROLLERS_PKG) ? controller : PlayUtils.CONTROLLERS_PKG + "." + controller;
    return JavaPsiFacade.getInstance(module.getProject()).findClass(className, GlobalSearchScope.moduleWithDependenciesScope(module));
  }

  @Nullable
  public static LocalSearchScope getRoutsFilesScope(@NotNull PsiElement element) {
    final Module module = ModuleUtilCore.findModuleForPsiElement(element);
    if (module != null) {
      final Set<PsiFile> files = getRoutesFiles(module);
      return new LocalSearchScope(files.toArray(PsiElement.EMPTY_ARRAY));
    }
    return null;
  }

  @NotNull
  public static Set<PsiFile> getRoutesFiles(@NotNull Module module) {
    Set<PsiFile> files = new HashSet<>();
    final Set<PsiDirectory> directories = getConfigDirectories(module);
    for (PsiDirectory directory : directories) {
      final PsiFile routes = directory.findFile("routes");
      if (routes != null) {
        files.add(routes);
      }
    }
    return files;
  }

  public static PsiMethod @NotNull [] getCorrespondingControllerMethods(@NotNull PsiFile psiFile) {
    Set<PsiMethod> psiMethods = new HashSet<>();
    final PsiClass controller = getCorrespondingController(psiFile);
    if (controller != null) {
      final String templateName = FileUtilRt.getNameWithoutExtension(psiFile.getName());
      for (PsiMethod psiMethod : controller.getAllMethods()) {
        if (StringUtil.toLowerCase(psiMethod.getName()).equals(StringUtil.toLowerCase(templateName))) {
          psiMethods.add(psiMethod); // IDEA-77111
        }
      }
    }
    return psiMethods.toArray(PsiMethod.EMPTY_ARRAY);
  }

  @Nullable
  public static PsiFile getCorrespondingView(@NotNull PsiMethod psiMethod) {
    final String fileName = psiMethod.getName();
    final PsiClass containingClass = psiMethod.getContainingClass();
    if (containingClass == null) return null;
    final String controllerPkgName = containingClass.getQualifiedName();
    if (controllerPkgName != null && controllerPkgName.startsWith(PlayUtils.CONTROLLERS_PKG)) {
      final String viewPkgName = controllerPkgName.replaceFirst(PlayUtils.CONTROLLERS_PKG, VIEWS_PKG);
      final PsiPackage viewPackage = JavaPsiFacade.getInstance(psiMethod.getProject()).findPackage(viewPkgName);
      if (viewPackage != null) {
        for (PsiDirectory psiDirectory : viewPackage.getDirectories()) {
          for (PsiFile psiFile : psiDirectory.getFiles()) {
            final VirtualFile virtualFile = psiFile.getVirtualFile();
            if (virtualFile != null &&
                StringUtil.toLowerCase(fileName).equals(StringUtil.toLowerCase(virtualFile.getNameWithoutExtension())) &&
                psiFile instanceof PlayPsiFile) {
              return psiFile;
            }
          }
        }
      }
    }
    return null;
  }


  public static Set<PsiDirectory> getConfigDirectories(@NotNull Module module) {
    Set<PsiDirectory> configs = new HashSet<>();

    collectConfigDirectories(module, configs);

    return configs;
  }

  public static void collectConfigDirectories(@NotNull Module module, @NotNull Set<? super PsiDirectory> configs) {
    collectConfigDirectories(module, configs, new HashSet<>());
  }

  public static void collectConfigDirectories(@NotNull Module module,
                                              @NotNull Set<? super PsiDirectory> configs,
                                              @NotNull Set<? super Module> visitedModules) {
    if (visitedModules.contains(module)) {
      return;
    }
    else {
      visitedModules.add(module);
    }

    final ModuleRootManager moduleRootManager = ModuleRootManager.getInstance(module);
    final VirtualFile[] roots = moduleRootManager.getContentRoots();
    for (VirtualFile root : roots) {
      final VirtualFile conf = root.findChild("conf");
      if (conf != null && conf.isDirectory()) {
        final PsiDirectory directory = PsiManager.getInstance(module.getProject()).findDirectory(conf);
        if (directory != null) {
          configs.add(directory);
        }
      }
    }

    for (Module dependentModule : moduleRootManager.getDependencies()) {
      collectConfigDirectories(dependentModule, configs, visitedModules);
    }
  }

  @NotNull
  public static Map<String, PsiFile> getCustomTags(@NotNull Module module) {
    Map<String, PsiFile> files = new HashMap<>();

    collectCustomTags(module, files);

    return files;
  }

  public static Set<PsiDirectory> getCustomTagRoots(@Nullable Module module) {
    if (module == null) return Collections.emptySet();

    return getCustomTagRoots(module, new HashSet<>());
  }

  public static Set<PsiDirectory> getCustomTagRoots(@NotNull Module module, @NotNull Set<? super Module> processed) {
    Set<PsiDirectory> directories = new HashSet<>();
    if (!processed.contains(module)) {
      final ModuleRootManager moduleRootManager = ModuleRootManager.getInstance(module);
      final VirtualFile[] roots = moduleRootManager.getSourceRoots();
      for (VirtualFile root : roots) {
        final VirtualFile tagsDir = root.findFileByRelativePath("views/tags");
        if (tagsDir != null && tagsDir.isDirectory()) {
          final PsiDirectory directory = PsiManager.getInstance(module.getProject()).findDirectory(tagsDir);
          if (directory != null) {
            directories.add(directory);
          }
        }
      }
      processed.add(module);
      for (Module dependentModule : moduleRootManager.getDependencies()) {
        directories.addAll(getCustomTagRoots(dependentModule, processed));
      }
    }
    return directories;
  }

  public static void collectCustomTags(@NotNull Module module, @NotNull Map<String, PsiFile> files) {
    collectCustomTags(module, files, new HashSet<>());
  }

  public static void collectCustomTags(@NotNull Module module,
                                       @NotNull Map<String, PsiFile> files,
                                       @NotNull Set<? super Module> processed) {
    if (processed.contains(module)) return;
    final ModuleRootManager moduleRootManager = ModuleRootManager.getInstance(module);
    final VirtualFile[] roots = moduleRootManager.getSourceRoots();
    for (VirtualFile root : roots) {
      final VirtualFile tagsDir = root.findFileByRelativePath("views/tags");
      if (tagsDir != null && tagsDir.isDirectory()) {
        final PsiDirectory directory = PsiManager.getInstance(module.getProject()).findDirectory(tagsDir);
        if (directory != null) {
          addFiles(files, directory, "");
        }
      }
    }
    processed.add(module);
    for (Module dependentModule : moduleRootManager.getDependencies()) {
      collectCustomTags(dependentModule, files, processed);
    }
  }

  private static void addFiles(@NotNull Map<String, PsiFile> files, @NotNull PsiDirectory directory, @NotNull String relatedPath) {
    for (PsiDirectory psiDirectory : directory.getSubdirectories()) {
      String dirName = psiDirectory.getName();
      addFiles(files, psiDirectory, (relatedPath.isEmpty() ? dirName : relatedPath + "." + dirName));
    }

    for (PsiFile file : directory.getFiles()) {
      if (file instanceof PlayPsiFile) {
        VirtualFile virtualFile = file.getVirtualFile();
        if (virtualFile != null) {
          String tagName = virtualFile.getNameWithoutExtension();
          files.put(relatedPath.isEmpty() ? tagName : relatedPath + "." + tagName, file);
        }
      }
    }
  }

  @Nullable
  public static PsiDirectory getViewsDirectory(@NotNull Module module) {
    final ModuleRootManager moduleRootManager = ModuleRootManager.getInstance(module);
    final VirtualFile[] roots = moduleRootManager.getSourceRoots();
    for (VirtualFile root : roots) {
      final VirtualFile viewsDir = root.findFileByRelativePath(VIEWS_PKG);
      if (viewsDir != null && viewsDir.isDirectory()) {
        return PsiManager.getInstance(module.getProject()).findDirectory(viewsDir);
      }
    }
    return null;
  }

  @NotNull
  public static Set<PlayFastTagDescriptor> getFastTags(@NotNull Module module) {
    Set<PlayFastTagDescriptor> fastTags = new HashSet<>();

    GlobalSearchScope scope = GlobalSearchScope.moduleWithDependenciesAndLibrariesScope(module);
    PsiClass fastTagsClass = JavaPsiFacade.getInstance(module.getProject()).findClass(PlayConstants.FAST_TAGS, scope);

    if (fastTagsClass != null) {
      Query<PsiClass> classQuery = ClassInheritorsSearch.search(fastTagsClass, scope, true);

      for (PsiClass psiClass : classQuery.findAll()) {
        fastTags.addAll(getFastTags(psiClass));
      }
    }
    return fastTags;
  }

  @NotNull
  public static Set<PlayFastTagDescriptor> getFastTags(@NotNull PsiClass psiClass) {
    Set<PlayFastTagDescriptor> fastTags = new HashSet<>();

    JamStringAttributeElement<String> namespace = getFastTagNamespace(psiClass);
    for (PsiMethod method : psiClass.getMethods()) {
      if (isFastTagMethod(method)) {
        fastTags.add(new PlayFastTagDescriptor(method, namespace));
      }
    }

    return fastTags;
  }

  @Nullable
  private static JamStringAttributeElement<String> getFastTagNamespace(@NotNull PsiClass aClass) {
    PsiAnnotation annotation = AnnotationUtil.findAnnotation(aClass, PlayConstants.FAST_TAGS_NAMESPACE);
    if (annotation == null) return null;
    return JamAttributeMeta.singleString("value").getJam(PsiElementRef.real(annotation));
  }

  private static boolean isFastTagMethod(@NotNull PsiMethod method) {
    if (method.getName().startsWith("_")) {
      PsiParameter[] parameters = method.getParameterList().getParameters();
      if (parameters.length == 5) {
        return true; // todo check parameters types: Map<?, ?> args, Closure body, PrintWriter out, ExecutableTemplate template, int fromLine
      }
    }
    return false;
  }
}