package com.intellij.play.utils.routes;

import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Set;

public final class RouterUtils {

  @NotNull
  public static Set<RouterLineDescriptor> getLineDescriptors(String content) {
    Set<RouterLineDescriptor> set = new HashSet<>();
    int lineOffset = 0;
    for (String line : content.split("\n")) {
      if (line.trim().length() != 0 && !line.startsWith("#")) {
        set.add(new RouterLineDescriptor(line, lineOffset));
      }
      lineOffset += line.length() + 1;
    }
    return set;
  }
}
