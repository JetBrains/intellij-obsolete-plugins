// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.plugins.grails.lang.gsp.highlighter;

import com.intellij.lang.html.HTMLLanguage;
import com.intellij.openapi.editor.JspHighlighterColors;
import com.intellij.openapi.editor.XmlHighlighterColors;
import com.intellij.openapi.editor.colors.EditorColorsScheme;
import com.intellij.openapi.editor.ex.util.LayerDescriptor;
import com.intellij.openapi.editor.ex.util.LayeredLexerEditorHighlighter;
import com.intellij.openapi.fileTypes.SyntaxHighlighter;
import com.intellij.openapi.fileTypes.SyntaxHighlighterFactory;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.templateLanguages.TemplateDataHighlighterWrapper;
import org.jetbrains.plugins.grails.lang.gsp.lexer.GspTokenTypesEx;
import org.jetbrains.plugins.grails.lang.gsp.lexer.core.GspTokenTypes;
import org.jetbrains.plugins.groovy.highlighter.GroovySyntaxHighlighter;

public class GspEditorHighlighter extends LayeredLexerEditorHighlighter {
  public GspEditorHighlighter(EditorColorsScheme scheme, Project project, VirtualFile virtualFile) {
    super(new GspSyntaxHighlighter(), scheme);

    // Register Groovy Highlighter
    SyntaxHighlighter groovyHighlighter = new GroovySyntaxHighlighter();
    final LayerDescriptor groovyLayer = new LayerDescriptor(groovyHighlighter, "\n", JspHighlighterColors.JSP_SCRIPTING_BACKGROUND);
    registerLayer(GspTokenTypes.GROOVY_CODE, groovyLayer);
    registerLayer(GspTokenTypes.GROOVY_EXPR_CODE, groovyLayer);
    registerLayer(GspTokenTypes.GSP_MAP_ATTR_VALUE, groovyLayer);
    registerLayer(GspTokenTypes.GROOVY_ATTR_VALUE, groovyLayer);
    registerLayer(GspTokenTypes.GROOVY_DECLARATION, groovyLayer);

    // Register html highlighter
    SyntaxHighlighter htmlHighlighter = SyntaxHighlighterFactory.getSyntaxHighlighter(HTMLLanguage.INSTANCE, project, virtualFile);
    final LayerDescriptor htmlLayer = new LayerDescriptor(new TemplateDataHighlighterWrapper(htmlHighlighter), "\n", XmlHighlighterColors.HTML_TAG);
    registerLayer(GspTokenTypesEx.GSP_TEMPLATE_DATA, htmlLayer);

    final SyntaxHighlighter directiveHighlighter = new GspDirectiveHighlighter();
    final LayerDescriptor directiveLayer = new LayerDescriptor(directiveHighlighter, "\n", JspHighlighterColors.JSP_ACTION_AND_DIRECTIVE_BACKGROUND);
    registerLayer(GspTokenTypes.GSP_DIRECTIVE, directiveLayer);
  }
}
