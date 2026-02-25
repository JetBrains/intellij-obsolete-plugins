// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package org.jetbrains.plugins.grails.lang.gsp.highlighter;

import com.intellij.openapi.fileTypes.FileType;
import com.intellij.psi.tree.IElementType;
import com.intellij.xml.impl.XmlBraceMatcher;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.grails.fileType.GspFileType;
import org.jetbrains.plugins.grails.lang.gsp.lexer.IGspElementType;
import org.jetbrains.plugins.grails.lang.gsp.lexer.core.GspTokenTypes;

public final class GspFileBraceMatcher extends XmlBraceMatcher {
  private static final int GSP_TOKEN_GROUP = 3;

  @Override
  public int getBraceTokenGroupId(final @NotNull IElementType tokenType) {
    if (tokenType instanceof IGspElementType) {
      return GSP_TOKEN_GROUP;
    }
    return super.getBraceTokenGroupId(tokenType);
  }

  @Override
  public boolean areTagsCaseSensitive(final @NotNull FileType fileType, final int braceGroupId) {
    if (braceGroupId == GSP_TOKEN_GROUP) return true;
    return super.areTagsCaseSensitive(fileType, braceGroupId);
  }

  @Override
  public boolean isStrictTagMatching(final @NotNull FileType fileType, final int braceGroupId) {
    if (braceGroupId == GSP_TOKEN_GROUP) return true;
    return super.isStrictTagMatching(fileType, braceGroupId);
  }

  @Override
  protected boolean isWhitespace(final IElementType tokenType1) {
    return tokenType1 == GspTokenTypes.GSP_WHITE_SPACE || super.isWhitespace(tokenType1);
  }

  @Override
  protected boolean isFileTypeWithSingleHtmlTags(final FileType fileType) {
    return fileType == GspFileType.GSP_FILE_TYPE || super.isFileTypeWithSingleHtmlTags(fileType);
  }
}
