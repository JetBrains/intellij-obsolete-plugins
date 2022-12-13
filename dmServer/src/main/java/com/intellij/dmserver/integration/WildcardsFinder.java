package com.intellij.dmserver.integration;

import com.google.common.base.CharMatcher;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;

public class WildcardsFinder {
  private static final CharMatcher WILDCARDS_MATCHER = CharMatcher.anyOf("*{}");

  private final String myPath;

  private final String myWildcards;

  public WildcardsFinder(String text) {
    int iWildcard = WILDCARDS_MATCHER.indexIn(text);
    if (iWildcard == -1) {
      myPath = FileUtil.toSystemIndependentName(text);
      myWildcards = "";
    }
    else {
      String noWildcardChars = FileUtil.toSystemIndependentName(text.substring(0, iWildcard));
      int iLastPart = noWildcardChars.lastIndexOf('/');
      myPath = iLastPart == -1 ? "" : noWildcardChars.substring(0, iLastPart);
      myWildcards = iLastPart == -1 ? text : text.substring(iLastPart + 1);
    }
  }

  public String getPath() {
    return myPath;
  }

  public String getWildcards() {
    return myWildcards;
  }

  public boolean hasWildcards() {
    return !StringUtil.isEmpty(myWildcards);
  }
}