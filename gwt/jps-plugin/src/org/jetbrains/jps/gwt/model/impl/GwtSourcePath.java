package org.jetbrains.jps.gwt.model.impl;

import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.SmartList;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.xmlb.annotations.Attribute;
import com.intellij.util.xmlb.annotations.Tag;
import com.intellij.util.xmlb.annotations.Transient;
import com.intellij.util.xmlb.annotations.XCollection;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import static com.intellij.util.containers.ContainerUtil.compareLexicographically;

@Tag("source")
public class GwtSourcePath implements Comparable<GwtSourcePath> {

  @Attribute("fullPath")
  public String myFullPath;

  @Attribute("caseSensitive")
  public boolean myCaseSensitive;

  @Attribute("defaultExcludes")
  public boolean myDefaultExcludes;

  @XCollection(propertyElementName = "includes", elementName = "include")
  public List<String> myIncludes;

  @XCollection(propertyElementName = "excludes", elementName = "exclude")
  public List<String> myExcludes;

  @Transient
  public Map<String, Pattern> myPatternsCache = new HashMap<>();

  @SuppressWarnings("unused")
  public GwtSourcePath() {
    myCaseSensitive = true;
    myDefaultExcludes = true;
    myIncludes = new SmartList<>();
    myExcludes = new SmartList<>();
  }

  public GwtSourcePath(@NotNull String fullPath, boolean caseSensitive, boolean defaultExcludes,
                       @NotNull List<String> includes, @NotNull List<String> excludes) {
    myFullPath = fullPath;
    myCaseSensitive = caseSensitive;
    myDefaultExcludes = defaultExcludes;
    myIncludes = copyOf(includes, caseSensitive);
    myExcludes = copyOf(excludes, caseSensitive);
  }

  private static @NotNull List<String> copyOf(@NotNull List<String> list, boolean caseSensitive) {
    if (list.isEmpty()) return list;

    List<String> copy = new ArrayList<>(list.size());
    for (String str : list) {
      copy.add(caseSensitive ? str : StringUtil.toLowerCase(str));
    }

    ContainerUtil.sort(copy);
    return copy;
  }

  public String getFullPath() {
    return myFullPath;
  }

  public boolean caseSensitive() {
    return myCaseSensitive;
  }

  public boolean defaultExcludes() {
    return myDefaultExcludes;
  }

  public boolean includes(String path) {
    if (myIncludes.isEmpty()) return true;
    for (String includePattern : myIncludes) {
      if (compilePattern(includePattern).matcher(path).matches()) {
        return true;
      }
    }
    return false;
  }

  public boolean excludes(String path) {
    for (String includePattern : myExcludes) {
      if (compilePattern(includePattern).matcher(path).matches()) {
        return true;
      }
    }
    return false;
  }

  private Pattern compilePattern(String str) {
    Pattern pattern = myPatternsCache.get(str);
    if (pattern == null) {
      pattern = Pattern.compile(FileUtil.convertAntToRegexp(str), myCaseSensitive ? 0 : Pattern.CASE_INSENSITIVE);
      myPatternsCache.put(str, pattern);
    }
    return pattern;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;

    GwtSourcePath that = (GwtSourcePath)obj;

    if (myCaseSensitive != that.myCaseSensitive) return false;
    if (myDefaultExcludes != that.myDefaultExcludes) return false;
    if (myCaseSensitive) {
      if (!myFullPath.equals(that.myFullPath)) return false;
    }
    else {
      if (!myFullPath.equalsIgnoreCase(that.myFullPath)) return false;
    }
    if (!myIncludes.equals(that.myIncludes)) return false;
    if (!myExcludes.equals(that.myExcludes)) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = (myCaseSensitive ? myFullPath : StringUtil.toLowerCase(myFullPath)).hashCode();
    result = 31 * result + (myCaseSensitive ? 1 : 0);
    result = 31 * result + (myDefaultExcludes ? 1 : 0);
    result = 31 * result + myIncludes.hashCode();
    result = 31 * result + myExcludes.hashCode();
    return result;
  }

  @Override
  public int compareTo(@NotNull GwtSourcePath that) {
    int compare = (myCaseSensitive == that.myCaseSensitive) ? 0 : (myCaseSensitive ? 1 : -1);
    if (compare != 0) return compare;

    compare = (myDefaultExcludes == that.myDefaultExcludes) ? 0 : (myDefaultExcludes ? 1 : -1);
    if (compare != 0) return compare;

    if (myCaseSensitive) {
      compare = myFullPath.compareTo(that.myFullPath);
    }
    else {
      compare = myFullPath.compareToIgnoreCase(that.myFullPath);
    }
    if (compare != 0) return compare;

    compare = compareLexicographically(myIncludes, that.myIncludes);
    if (compare != 0) return compare;

    return compareLexicographically(myExcludes, that.myExcludes);
  }
}