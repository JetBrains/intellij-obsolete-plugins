package com.intellij.gwt.clientBundle.css.language;

import com.intellij.gwt.clientBundle.css.language.psi.impl.GwtCssFileImpl;
import com.intellij.lang.PsiParser;
import com.intellij.lang.css.CSSParserDefinition;
import com.intellij.openapi.project.Project;
import com.intellij.psi.FileViewProvider;
import com.intellij.psi.PsiFile;
import com.intellij.psi.tree.IFileElementType;
import org.jetbrains.annotations.NotNull;

public final class GwtCssParserDefinition extends CSSParserDefinition {
  @Override
  public @NotNull PsiFile createFile(@NotNull FileViewProvider viewProvider) {
    return new GwtCssFileImpl(viewProvider);
  }

  @Override
  public @NotNull IFileElementType getFileNodeType() {
    return GwtCssStubElementTypes.GWT_CSS_FILE;
  }

  @Override
  public @NotNull PsiParser createParser(Project project) {
    return new GwtCssParser();
  }
}
