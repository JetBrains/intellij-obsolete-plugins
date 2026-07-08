package com.intellij.lang.puppet.ide.libraries;

import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.regex.Pattern;

public final class PuppetLibraryNamingUtil {
  private static final @NonNls String OLD_PUPPET_LIB_NAME = "Puppet modules";

  private static final @NonNls String PUPPET_LIB_TAIL = "[puppet module]";

  public static @NotNull String getLibraryNameByNameAndVersion(@NotNull String name, @NotNull String version) {
    return name + "_" + version + " " + PUPPET_LIB_TAIL;
  }

  public static boolean isPuppetLibraryName(@Nullable String libraryName) {
    if (libraryName == null) {
      return false;
    }

    return libraryName.equals(OLD_PUPPET_LIB_NAME)
           || libraryName.equals(getLibraryNameForStubLib())
           || libraryName.matches("^\\S*_\\S* " + Pattern.quote(PUPPET_LIB_TAIL) + "$");
  }

  public static @NotNull String getLibraryNameForStubLib() {
    return "stub " + PUPPET_LIB_TAIL;
  }

  static @NotNull String getModuleFallbackLibraryName(@NotNull String url) {
    final List<String> list = StringUtil.split(url, "/");
    final String dirName = StringUtil.notNullize(ContainerUtil.getLastItem(list), url);

    return getLibraryNameByNameAndVersion(dirName, "nover");
  }
}
