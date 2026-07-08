package com.intellij.lang.puppet.util;

import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public final class PuppetQualifiedNamesUtil {
  public static final String SEPARATOR = "::";
  public static final String MAIN_NAMESPACE = "";

  public static String capitalizePuppetName(@NonNls String typeName) {
    char[] chars = typeName.toCharArray();
    int sepcount = 0;
    for (int i = 0; i < chars.length; i++) {
      if (i == 0 || sepcount == 2) {
        chars[i] = Character.toUpperCase(chars[i]);
        sepcount = 0;
      }
      else if (chars[i] == ':') {
        sepcount++;
      }
      else {
        chars[i] = Character.toLowerCase(chars[i]);
        sepcount = 0;
      }
    }
    return new String(chars);
  }

  /**
   * If variable name starts with separator it's defined in the main:: namespace
   *
   * @param name fqn with possible leading top level NS prefix ::
   * @return fqn with main:: prefix if necessary
   */
  @Contract("null -> null")
  public static String getCanonicalVariableFullQualifiedName(@Nullable String name) {
    if (name == null) {
      return null;
    }

    if (!StringUtil.startsWith(name, SEPARATOR)) {
      return name;
    }

    return name.lastIndexOf(SEPARATOR) == 0 ?
           MAIN_NAMESPACE + name :  // $::varname
           name.substring(2);       // $::some::class::varname
  }

  public static @NotNull List<TextRange> getQualifiedPartRanges(@NotNull PsiElement element, TextRange nameRange) {
    final String text = element.getText();
    final List<TextRange> result = new ArrayList<>();

    int last = nameRange.getStartOffset();
    while (last < nameRange.getEndOffset()) {
      int next = text.indexOf(SEPARATOR, last);
      if (next == -1) {
        next = nameRange.getEndOffset();
      }

      if (next > last) {
        result.add(TextRange.create(last, next));
      }
      last = next + SEPARATOR.length();
    }
    return result;
  }
}
