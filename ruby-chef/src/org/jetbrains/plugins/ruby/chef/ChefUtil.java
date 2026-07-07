package org.jetbrains.plugins.ruby.chef;

import com.intellij.execution.Platform;
import com.intellij.ide.projectView.impl.ProjectRootsUtil;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.ruby.chef.sourceRoot.CookbookUrlsCache;
import org.jetbrains.plugins.ruby.gem.GemInfo;
import org.jetbrains.plugins.ruby.gem.util.RubyGemSearchUtil;
import org.jetbrains.plugins.ruby.ruby.RModuleUtil;
import org.jetbrains.plugins.ruby.ruby.lang.psi.RPsiElement;

public final class ChefUtil {
  public static final String RESOURCE = "Resource";
  public static final String CHEF_RESOURCE = "Chef::" + RESOURCE;
  public static final String CHEF_GEM = "chef";

  public static boolean isCookbook(@Nullable PsiDirectory directory) {
    return directory != null && isInCookbook(directory) && ProjectRootsUtil.isSourceRoot(directory);
  }

  public static boolean isInCookbook(@Nullable PsiElement element) {
    if (element == null) return false;
    final PsiDirectory directory;
    if (element instanceof PsiDirectory) {
      directory = (PsiDirectory)element;
    }
    else {
      PsiFile file = element.getContainingFile();
      if (file == null) return false;

      directory = file.getOriginalFile().getContainingDirectory();
    }

    final Module module = ModuleUtilCore.findModuleForPsiElement(element);
    if (directory == null || module == null) return false;

    final String url = directory.getVirtualFile().getUrl();

    return CookbookUrlsCache.Companion.getInstance(module.getProject())
      .getCachedURLs(module).stream()
      .anyMatch(sourceRoot -> url.startsWith(sourceRoot));
  }

  public static @Nullable GemInfo findChefGem(RPsiElement file) {
    final Module module = ModuleUtilCore.findModuleForPsiElement(file);
    if (module == null) return null;

    if (!RModuleUtil.getInstance().isRubyModule(module)) return null;

    final Sdk sdk = RModuleUtil.getInstance().findRubySdkForModule(module);
    return RubyGemSearchUtil.findGem(module, sdk, CHEF_GEM);
  }

  public static String getCookbookNameByUrl(final @NotNull String url) {
    final int lastPathSeparator = url.lastIndexOf(Platform.current().fileSeparator);
    return lastPathSeparator == -1 ? url : url.substring(lastPathSeparator + 1);
  }

  public static @Nullable PsiDirectory getCookbookByFileInside(final @NotNull PsiFile file) {
    final PsiElement cookbook = PsiTreeUtil.findFirstParent(file, element -> element instanceof PsiDirectory && isCookbook(((PsiDirectory)element)));

    return cookbook instanceof PsiDirectory ? ((PsiDirectory)cookbook) : null;
  }
}
