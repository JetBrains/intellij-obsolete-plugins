package com.intellij.dmserver.install.impl;

import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DMServerLibraryFinder {
  @NonNls
  private static final Pattern VERSION_SUFFIX = Pattern.compile("(-|_)\\d+(\\.\\w+)*\\.jar");
  @NonNls
  private static final Pattern SOURCES_SUFFIX = Pattern.compile("sources(-|_)\\d+(\\.\\w+)*\\.jar");

  public List<VirtualFile> getLibraryBundles(VirtualFile folder, boolean filterSources) {
    if (folder == null || !folder.exists()) {
      return Collections.emptyList();
    }
    List<VirtualFile> result = new ArrayList<>();
    for (VirtualFile next : folder.getChildren()) {
      if ("jar".equals(next.getExtension())) {
        if (isSourcesBundle(next) && filterSources) {
          continue;
        }
        result.add(next);
      }
    }
    return result;
  }

  @Nullable
  public VirtualFile getSourcesForLibraryBundle(@NotNull VirtualFile bundle) {
    VirtualFile parentDir = getSourcesContainer(bundle);
    String libraryFilename = bundle.getName();

    int versionStart = libraryFilename.lastIndexOf("-");
    if (versionStart < 0) {
      versionStart = libraryFilename.lastIndexOf("_");
    }
    if (versionStart > 0) {
      String versionSuffix = libraryFilename.substring(versionStart);
      String baseName = libraryFilename.substring(0, versionStart);
      Matcher matcher = VERSION_SUFFIX.matcher(versionSuffix);
      if (matcher.matches()) {
        String minusOrUnderscore = matcher.group(1);
        @NonNls
        String sourceFileName = baseName + minusOrUnderscore + "sources" + versionSuffix;
        VirtualFile source = parentDir.findChild(sourceFileName);
        if (source != null) {
          return source;
        }
      }
    }
    return null;
  }

  public boolean isSourcesBundle(@NotNull VirtualFile jar) {
    String name = jar.getName();
    return SOURCES_SUFFIX.matcher(name).find();
  }

  protected VirtualFile getSourcesContainer(VirtualFile bundle) {
    return bundle.getParent();
  }


}
