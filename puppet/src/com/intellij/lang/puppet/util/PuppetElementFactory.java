package com.intellij.lang.puppet.util;

import com.intellij.lang.puppet.PuppetFileType;
import com.intellij.lang.puppet.psi.PuppetQuotedString;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiFileFactory;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;

public final class PuppetElementFactory {
  public static @NotNull PsiElement createLeafElement(Project project, String expression, int position) {
    final PsiFile file = createDummyFile(project, expression);
    PsiElement result = file.findElementAt(position);

    if (result == null) {
      throw new RuntimeException("Cannot instantiate var");
    }

    return result.getParent();
  }

  public static @NotNull PuppetQuotedString createQuotedStringElementWithContent(Project project, String content) {
    return createQuotedStringElement(project, "'" + smartEscapeChar(content, '\'') + "'");
  }

  // StringUtil.escape is not checking if symbol is already escaped
  public static String smartEscapeChar(@NotNull String source, char charToEscape) {
    return StringUtil.escapeChar(StringUtil.escapeChar(source, '\\'), charToEscape);
  }

  public static @NotNull PuppetQuotedString createQuotedStringElement(Project project, String newString) {
    PuppetQuotedString result = PsiTreeUtil.findChildOfType(createDummyFile(project, newString), PuppetQuotedString.class);
    assert result != null : "Got incorrect result with: " + newString;
    return result;
  }

  public static PsiFile createDummyFile(Project myProject, String text) {
    final PsiFileFactory factory = PsiFileFactory.getInstance(myProject);
    final String name = "dummy." + PuppetFileType.INSTANCE.getDefaultExtension();
    return factory.createFileFromText(name, PuppetFileType.INSTANCE, text);
  }
}
