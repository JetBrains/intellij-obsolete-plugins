package com.intellij.lang.puppet.ide.refactoring;

import com.intellij.lang.puppet.lexer.PuppetLexerKeywords;
import com.intellij.lang.refactoring.NamesValidator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import org.jetbrains.annotations.NotNull;

public class PuppetNamesValidator implements NamesValidator {
  @Override
  public boolean isKeyword(@NotNull String name, Project project) {
    return PuppetLexerKeywords.getKeywordsMap(project).containsKey(name);
  }

  @Override
  public boolean isIdentifier(@NotNull String name, Project project) {
    return !StringUtil.isEmptyOrSpaces(name);
  }
}
