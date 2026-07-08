package com.intellij.lang.puppet.lexer;

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PuppetHeredocLexerAdapter extends PuppetLexerAdapter {
  public PuppetHeredocLexerAdapter(@Nullable Project project) {
    super(project);
  }

  @Override
  public void start(@NotNull CharSequence buffer, int startOffset, int endOffset, int initialState) {
    super.start(buffer, startOffset, endOffset, _PuppetLexer.HEREDOC_QQ);
  }
}

