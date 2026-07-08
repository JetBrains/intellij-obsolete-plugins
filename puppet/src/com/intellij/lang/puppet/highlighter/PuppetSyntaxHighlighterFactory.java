package com.intellij.lang.puppet.highlighter;

import com.intellij.openapi.fileTypes.SyntaxHighlighter;
import com.intellij.openapi.fileTypes.SyntaxHighlighterFactory;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

/**
 * @author Anna Bulenkova
 */
public class PuppetSyntaxHighlighterFactory extends SyntaxHighlighterFactory {
  @Override
  public @NotNull SyntaxHighlighter getSyntaxHighlighter(Project project, VirtualFile virtualFile) {
    return new PuppetHighlighter(project);
  }
}
