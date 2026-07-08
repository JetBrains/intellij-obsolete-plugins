package com.intellij.lang.puppet.ide.libraries;

import com.intellij.lang.puppet.psi.PuppetLazyProxyLightElement;
import com.intellij.openapi.application.PathManager;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileSystem;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.VisibleForTesting;

import java.io.File;
import java.util.Arrays;

public final class PuppetLibraryUtil {
  public static final @NonNls String PUPPET_STUBS_ROOT_PATH = getPuppetPluginPath() + "/lib/stubs/";
  public static final @NonNls String PUPPET_METAPARAMETERS_STUB_TYPE_NAME = "stub__metaparams__";
  private static final @NonNls String PUPPET_BUILTIN_FUNCTIONS_STUBS_FILE_NAME = "stub_functions.rb";

  private static final String PUPPET_BUILTIN_FUNCTIONS_STUBS_FILE_PATH = PUPPET_STUBS_ROOT_PATH + PUPPET_BUILTIN_FUNCTIONS_STUBS_FILE_NAME;

  public static @Nullable VirtualFile getStubsRoot() {
    return getStubsRoot(false);
  }

  /**
   * @apiNote please check restrictions for {@link VirtualFileSystem#refreshAndFindFileByPath(String)} if going to invoke this with {@code refresh} flag.
   */
  @VisibleForTesting
  public static @Nullable VirtualFile getStubsRoot(boolean refresh) {
    return VfsUtil.findFileByIoFile(new File(PUPPET_STUBS_ROOT_PATH), refresh);
  }

  public static boolean isFunctionsStubsFile(@NotNull VirtualFile file) {
    return FileUtil.namesEqual(file.getName(), PUPPET_BUILTIN_FUNCTIONS_STUBS_FILE_NAME) &&
           FileUtil.pathsEqual(file.getPath(), PUPPET_BUILTIN_FUNCTIONS_STUBS_FILE_PATH);
  }



  public static boolean isFunctionStubElement(@NotNull PsiElement element) {
    VirtualFile elementVirtualFile = element instanceof PuppetLazyProxyLightElement ?
                                     ((PuppetLazyProxyLightElement)element).getVirtualFile() :
                                     element.getContainingFile().getVirtualFile();

    return elementVirtualFile != null && isFunctionsStubsFile(elementVirtualFile);
  }


  private static String getPuppetPluginPath() {
    String[] variants = {
      PathManager.getPreInstalledPluginsPath(),
      PathManager.getPluginsPath()
    };

    for (String variant : variants) {
      String path = variant + "/puppet";
      if (FileUtil.exists(path)) {
        return path;
      }
    }
    throw new AssertionError("Could not set up testlib: could not find plugin paths among: " + Arrays.toString(variants));
  }
}
