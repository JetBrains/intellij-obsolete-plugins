/*
 * Copyright (c) 2000-2005 by JetBrains s.r.o. All Rights Reserved.
 * Use is subject to license terms.
 */
package com.intellij.play.language;

import com.intellij.lang.StdLanguages;
import com.intellij.lang.html.HTMLLanguage;
import com.intellij.openapi.editor.JspHighlighterColors;
import com.intellij.openapi.editor.colors.EditorColorsScheme;
import com.intellij.openapi.editor.ex.util.LayerDescriptor;
import com.intellij.openapi.editor.ex.util.LayeredLexerEditorHighlighter;
import com.intellij.openapi.fileTypes.SyntaxHighlighter;
import com.intellij.openapi.fileTypes.SyntaxHighlighterFactory;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.templateLanguages.TemplateDataHighlighterWrapper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.groovy.highlighter.GroovySyntaxHighlighter;


public class PlayEditorHighlighter extends LayeredLexerEditorHighlighter{

  public PlayEditorHighlighter(@Nullable final Project project,
                               @Nullable final VirtualFile virtualFile,
                               @NotNull final EditorColorsScheme colors) {
    super(new PlaySyntaxHighlighter(), colors);

    // Register Groovy Highlighter
    SyntaxHighlighter groovyHighlighter = new GroovySyntaxHighlighter();
    final LayerDescriptor groovyLayer = new LayerDescriptor(groovyHighlighter, "\n", JspHighlighterColors.JSP_SCRIPTING_BACKGROUND);

    registerLayer(PlayElementTypes.GROOVY_SCRIPT, groovyLayer);
    registerLayer(PlayElementTypes.EL_EXPRESSION, groovyLayer);
    registerLayer(PlayElementTypes.ACTION_SCRIPT, groovyLayer);
    registerLayer(PlayElementTypes.TAG_EXPRESSION, groovyLayer);

    // Register html highlighter
    SyntaxHighlighter htmlHighlighter = SyntaxHighlighterFactory.getSyntaxHighlighter(HTMLLanguage.INSTANCE, project, virtualFile);
    final LayerDescriptor htmlLayer = new LayerDescriptor(new TemplateDataHighlighterWrapper(htmlHighlighter), "\n");
    registerLayer(PlayElementTypes.TEMPLATE_TEXT, htmlLayer);
  }
}
