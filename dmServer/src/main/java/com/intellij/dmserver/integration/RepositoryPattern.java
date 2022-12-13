package com.intellij.dmserver.integration;

import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.regex.Pattern;

public class RepositoryPattern {

  public static final String ANY_FILE = "*";

  public static final String RECURSIVE_DIRS = "**";

  public static final Pattern RECURSIVE_PATTERN_MARK = null;

  private static final Pattern BRANCHES_PATTERN = Pattern.compile("\\{.*?\\}");

  public static RepositoryPattern create(DMServerRepositoryItem source, String searchPattern) {
    searchPattern = BRANCHES_PATTERN.matcher(searchPattern).replaceAll(ANY_FILE);
    int filePatternStart = searchPattern.lastIndexOf('/');
    boolean hasFilePattern = filePatternStart != -1;
    String filePattern = hasFilePattern ? searchPattern.substring(filePatternStart + 1) : searchPattern;
    String dirs = hasFilePattern ? searchPattern.substring(0, filePatternStart) : "";

    WildcardsFinder wildcardsFinder = new WildcardsFinder(dirs);
    List<String> dirPatterns = wildcardsFinder.hasWildcards()
                               ? StringUtil.split(wildcardsFinder.getWildcards(), "/")
                               : Collections.emptyList();

    return new RepositoryPattern(source,
                                 wildcardsFinder.getPath(),
                                 dirPatterns,
                                 filePattern.isEmpty() ? ANY_FILE : filePattern);
  }


  private final DMServerRepositoryItem mySource;
  private final String myFixedPath;
  private final List<Pattern> myDirPatterns;
  private final Pattern myFilePattern;

  private final String myFullPattern;

  public RepositoryPattern(DMServerRepositoryItem source, String fixedPath, List<String> dirPatterns, String filePattern) {
    mySource = source;
    myFixedPath = fixedPath;
    myDirPatterns = new ArrayList<>();
    for (String dirPattern : dirPatterns) {
      myDirPatterns.add(RECURSIVE_DIRS.equals(dirPattern) ? RECURSIVE_PATTERN_MARK : buildPattern(dirPattern));
    }

    myFilePattern = buildPattern(filePattern);

    StringBuilder fullPatternBuilder = new StringBuilder();
    fullPatternBuilder.append(fixedPath);
    if (!fixedPath.endsWith("/")) {
      fullPatternBuilder.append("/");
    }
    for (String dirPattern : dirPatterns) {
      fullPatternBuilder.append(dirPattern);
      fullPatternBuilder.append("/");
    }
    fullPatternBuilder.append(filePattern);

    myFullPattern = fullPatternBuilder.toString();
  }

  private static Pattern buildPattern(String patternText) {
    @NonNls String quoted = "\\Q" + patternText + "\\E";
    return Pattern.compile(quoted.replace(ANY_FILE, "\\E.*\\Q"));
  }

  public String getFullPattern() {
    return myFullPattern;
  }

  @Nullable
  public VirtualFile findBaseDir() {
    return LocalFileSystem.getInstance().refreshAndFindFileByPath(myFixedPath);
  }

  public List<VirtualFile> collectFiles() {
    VirtualFile baseDir = findBaseDir();
    if (baseDir == null) {
      return Collections.emptyList();
    }

    Set<VirtualFile> files = new HashSet<>();
    walkDirPattern(files, 0, baseDir);
    return new ArrayList<>(files);
  }

  private void walkDirPattern(Set<VirtualFile> files, int iDirPattern, VirtualFile baseDir) {
    baseDir.refresh(false, false);
    if (iDirPattern == myDirPatterns.size()) {
      for (VirtualFile childFile : baseDir.getChildren()) {
        if (!childFile.isDirectory() && myFilePattern.matcher(childFile.getName()).matches()) {
          files.add(childFile);
        }
      }
    }
    else {
      Pattern dirPattern = myDirPatterns.get(iDirPattern);
      if (dirPattern == RECURSIVE_PATTERN_MARK) {
        walkDirPattern(files, iDirPattern + 1, baseDir);
        for (VirtualFile childFile : baseDir.getChildren()) {
          if (childFile.isDirectory()) {
            walkDirPattern(files, iDirPattern, childFile);
          }
        }
      }
      else {
        for (VirtualFile childFile : baseDir.getChildren()) {
          if (childFile.isDirectory() && dirPattern.matcher(childFile.getName()).matches()) {
            walkDirPattern(files, iDirPattern + 1, childFile);
          }
        }
      }
    }
  }

  public DMServerRepositoryItem getSource() {
    return mySource;
  }

  public boolean hasDirPatterns() {
    return !myDirPatterns.isEmpty();
  }
}
