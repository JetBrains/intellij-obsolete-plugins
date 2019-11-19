package com.jetbrains.plugins.compass;

import com.intellij.execution.configurations.PathEnvironmentVariableUtil;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.util.EnvironmentUtil;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.*;
import java.util.regex.Pattern;

import static com.intellij.openapi.vfs.VfsUtilCore.pathToUrl;

public class GemUtil {
  private static final String GEMS_DIRECTORY = "gems";

  private static final String RBENV_HOME_DIRECTORY_NAME = ".rbenv";
  private static final String RBENV_HOME_VARIABLE = "RBENV_ROOT";
  private static final String RBENV_RUBIES_DIRECTORY = "versions";
  private static final String RBENV_GEMS_RELATIVE_PATH = "lib/ruby/gems";

  private static final String RVM_HOME_DIRECTORY_NAME = ".rvm";
  private static final String RVM_HOME_VARIABLE = "rvm_path";
  private static final String RVM_SYSTEM_WIDE_PATH = "/usr/local/rvm";
  private static final String RVM_RUBIES_DIRECTORY = "rubies";


  @NotNull
  public static Collection<? extends VirtualFile> findGems(@NotNull String gemName) {
    final Collection<VirtualFile> result = new LinkedHashSet<>();
    result.addAll(findRvmGems(gemName));
    result.addAll(findRbenvGems(gemName));
    return result;
  }

  @NotNull
  public static List<String> listPossibleExecutableFilePaths(@NotNull String executableFileName, @NotNull String executableRelativePath) {
    return ContainerUtil.map(listPossibleExecutableFiles(executableFileName, executableRelativePath), file -> file.getPath());
  }

  public static boolean isValidExecutableFile(@Nullable VirtualFile executableFile) {
    return executableFile != null && executableFile.exists() && !executableFile.isDirectory();
  }

  @NotNull
  public static Set<VirtualFile> listPossibleExecutableFiles(@NotNull String executableFileName, @NotNull String executableRelativePath) {
    Set<VirtualFile> interpreters = new LinkedHashSet<>();
    final List<File> path = PathEnvironmentVariableUtil.findAllExeFilesInPath(executableFileName);
    for (File file : path) {
      final VirtualFile compassExecutableFile = VfsUtil.findFileByIoFile(file, true);
      if (isValidExecutableFile(compassExecutableFile)) {
        interpreters.add(compassExecutableFile);
      }
    }
    for (VirtualFile gemDirectory : findGems(executableFileName)) {
      populateWithExecutableFilesFromGemDirectory(gemDirectory, interpreters, executableRelativePath);
    }
    return interpreters;
  }

  @NotNull
  private static Collection<? extends VirtualFile> findRbenvGems(@NotNull String gemName) {
    if (SystemInfo.isUnix) {
      VirtualFile rbenvHome = getRbenvHome();
      if (rbenvHome != null) {
        final VirtualFile versionsDirectory = rbenvHome.findFileByRelativePath(RBENV_RUBIES_DIRECTORY);
        if (versionsDirectory != null && versionsDirectory.isDirectory()) {
          final Collection<VirtualFile> result = new LinkedHashSet<>();
          final Pattern gemNamePattern = createGemNamePattern(gemName);

          for (VirtualFile versionDirectory : versionsDirectory.getChildren()) {
            final VirtualFile versionGemsDirectory = versionDirectory.findFileByRelativePath(RBENV_GEMS_RELATIVE_PATH);
            if (versionGemsDirectory != null && versionGemsDirectory.isDirectory()) {
              for (VirtualFile subversionDirectory : versionGemsDirectory.getChildren()) {

                final VirtualFile gemsDirectory = subversionDirectory.findFileByRelativePath(GEMS_DIRECTORY);
                if (gemsDirectory != null && gemsDirectory.isDirectory()) {
                  for (VirtualFile gem : gemsDirectory.getChildren()) {
                    if (gem.isDirectory() && gemNamePattern.matcher(gem.getName()).matches()) {
                      result.add(gem);
                    }
                  }
                }
              }
            }
          }

          return result;
        }
      }
    }
    return Collections.emptySet();
  }

  @NotNull
  private static Collection<? extends VirtualFile> findRvmGems(@NotNull String gemName) {
    if (SystemInfo.isUnix) {
      VirtualFile rvmHome = getRvmHome();
      if (rvmHome != null) {
        final VirtualFile versionsDirectory = rvmHome.findChild(GEMS_DIRECTORY);
        if (versionsDirectory != null && versionsDirectory.isDirectory() && versionsDirectory.getChildren().length > 0) {
          final HashSet<VirtualFile> result = new LinkedHashSet<>();
          final Pattern gemNamePattern = createGemNamePattern(gemName);

          for (VirtualFile rubyDirectory : versionsDirectory.getChildren()) {
            final VirtualFile gemsDirectory = rubyDirectory.findFileByRelativePath(GEMS_DIRECTORY);
            if (gemsDirectory != null && gemsDirectory.isDirectory()) {
              for (VirtualFile gem : gemsDirectory.getChildren()) {
                if (gem.isDirectory() && gemNamePattern.matcher(gem.getName()).matches()) {
                  result.add(gem);
                }
              }
            }
          }
          return result;
        }
      }
    }
    return Collections.emptySet();
  }

  @Nullable
  private static VirtualFile getRbenvHome() {
    final VirtualFile rubyHome = findRubyHomeFromEnvironmentVariable(RBENV_HOME_VARIABLE);
    return rubyHome != null ? rubyHome : findRubyHomeInUserDirectory(RBENV_HOME_DIRECTORY_NAME);
  }


  @Nullable
  private static VirtualFile getRvmHome() {
    VirtualFile rvmHome = findRubyHomeFromEnvironmentVariable(RVM_HOME_VARIABLE);
    if (rvmHome == null) {
      rvmHome = findRubyHomeInUserDirectory(RVM_HOME_DIRECTORY_NAME);
      if (rvmHome == null) {
        final VirtualFile systemWideRvmHome = VirtualFileManager.getInstance().refreshAndFindFileByUrl(RVM_SYSTEM_WIDE_PATH);
        if (isValidRubyHome(systemWideRvmHome)) {
          assert systemWideRvmHome != null;
          final VirtualFile markerDirectory = systemWideRvmHome.findChild(RVM_RUBIES_DIRECTORY);
          if (isValidRubyHome(markerDirectory)) {
            rvmHome = systemWideRvmHome;
          }
        }
      }
    }
    return rvmHome;
  }

  @Nullable
  private static VirtualFile findRubyHomeFromEnvironmentVariable(@NotNull String variableName) {
    final String pathFromEnvironmentVariable = EnvironmentUtil.getValue(variableName);
    if (!StringUtil.isEmpty(pathFromEnvironmentVariable)) {
      String possibleRubyHomePath = pathToUrl(pathFromEnvironmentVariable);
      final VirtualFile rubyHome = VirtualFileManager.getInstance().refreshAndFindFileByUrl(possibleRubyHomePath);
      if (isValidRubyHome(rubyHome)) {
        return rubyHome;
      }
    }
    return null;
  }

  @Nullable
  private static VirtualFile findRubyHomeInUserDirectory(@NotNull String rubyHomeDirectoryName) {
    final VirtualFile userHomeFolder = VfsUtil.getUserHomeDir();
    if (userHomeFolder != null) {
      VirtualFile rvmHome = userHomeFolder.findChild(rubyHomeDirectoryName);
      if (isValidRubyHome(rvmHome)) {
        return rvmHome;
      }
    }
    return null;
  }

  @NotNull
  private static Pattern createGemNamePattern(@NotNull String gemName) {
    return Pattern.compile(StringUtil.escapePattern(gemName) + "-\\d.*");
  }

  private static boolean isValidRubyHome(@Nullable VirtualFile rvmHome) {
    if (rvmHome == null) {
      return false;
    }
    return rvmHome.exists() && rvmHome.isDirectory() && rvmHome.getChildren().length > 0;
  }

  private static void populateWithExecutableFilesFromGemDirectory(@NotNull VirtualFile gemsDirectory,
                                                                  @NotNull Set<VirtualFile> interpreters,
                                                                  @NotNull String relativePathInGem) {
    final VirtualFile compassExecutableFile = gemsDirectory.findFileByRelativePath(relativePathInGem);
    if (isValidExecutableFile(compassExecutableFile)) {
      interpreters.add(compassExecutableFile);
    }
  }

  private GemUtil() {
  }
}
