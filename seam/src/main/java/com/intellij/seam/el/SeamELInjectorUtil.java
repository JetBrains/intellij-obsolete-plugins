package com.intellij.seam.el;

import com.intellij.javaee.el.impl.ELUtil;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public final class SeamELInjectorUtil {
  private SeamELInjectorUtil() {
  }

  @NotNull
  public static List<TextRange> getELTextRanges(final PsiElement element) {
    return ELUtil.getELTextRanges(element, "#{", "}");
  }
}
