package com.intellij.gwt.module.model.impl;

import com.intellij.openapi.util.io.FileUtil;
import com.intellij.util.ArrayUtilRt;
import org.jetbrains.annotations.NonNls;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

import static com.intellij.openapi.util.text.StringUtil.split;
import static com.intellij.openapi.util.text.StringUtil.toLowerCase;

public final class GwtFilePatternUtil {
  private static final @NonNls String[] EXCLUDED_FILES = {
      "CVS", ".cvsignore", "SCCS", "vssver.scc",
      ".svn", ".DS_Store",
      ".git", ".gitattributes", ".gitignore", ".gitmodules",
      ".hg", ".hgignore", ".hgsub", ".hgsubstate", ".hgtags",
      ".bzr", ".bzrignore"
  };

  private static final @NonNls String[] EXCLUDED_FOLDERS = {
    "CVS", "SCCS", ".svn", ".git", ".hg", ".bzr"
  };

  private static final Set<String> ourExcludedFiles = new HashSet<>();
  private static final Set<String> ourCaseInsensitiveExcludedFiles = new HashSet<>();

  private static final Set<String> ourExcludedFolders = new HashSet<>();
  private static final Set<String> ourCaseInsensitiveExcludedFolders = new HashSet<>();

  static {
    for (String fileName : EXCLUDED_FILES) {
      ourExcludedFiles.add(fileName);
      ourCaseInsensitiveExcludedFiles.add(toLowerCase(fileName));
    }
    for (String fileName : EXCLUDED_FOLDERS) {
      ourExcludedFolders.add(fileName);
      ourCaseInsensitiveExcludedFolders.add(toLowerCase(fileName));
    }
  }

  private static boolean isFileExcluded(String fileName) {
    if (fileName.startsWith(".#")) return true;
    if (fileName.startsWith("._")) return true;
    if (fileName.endsWith("~")) return true;
    int length = fileName.length();
    if (length >= 2) {
      if (fileName.charAt(0) == fileName.charAt(length - 1)) {
        return fileName.charAt(0) == '#' || fileName.charAt(0) == '%';
      }
    }
    return false;
  }

  private GwtFilePatternUtil() {
  }

  public static Pattern createPattern(String pattern, boolean caseSensitive) {
    return Pattern.compile(FileUtil.convertAntToRegexp(pattern), caseSensitive ? 0 : Pattern.CASE_INSENSITIVE);
  }

  public static boolean isExcludedByDefault(String path, boolean caseSensitive) {
    if (!caseSensitive) {
      path = toLowerCase(path);
    }

    String[] names = ArrayUtilRt.toStringArray(split(path, "/"));
    int length = names.length;
    if (length == 0) return false;

    Set<String> excludedFiles = caseSensitive ? ourExcludedFiles : ourCaseInsensitiveExcludedFiles;
    String fileName = names[length - 1];
    if (excludedFiles.contains(fileName) || isFileExcluded(fileName)) return true;

    Set<String> excludedFolders = caseSensitive ? ourExcludedFolders : ourCaseInsensitiveExcludedFolders;
    for (int i = 0; i < length - 1; i++) {
      if (excludedFolders.contains(names[i])) return true;
    }

    return false;
  }
}
