package com.intellij.lang.puppet.lexer;

import com.intellij.openapi.project.Project;

public class PuppetLexer extends _PuppetLexer {
  public PuppetLexer(Project project) {
    super(project);
  }

  @Override
  public void reset(CharSequence buffer, int start, int end, int initialState) {
    super.reset(buffer, start, end, initialState);
    clearOnReset();
  }
}
