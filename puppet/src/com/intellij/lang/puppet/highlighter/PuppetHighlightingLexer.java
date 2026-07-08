package com.intellij.lang.puppet.highlighter;

import com.intellij.lang.puppet.lexer.PuppetHeredocLexerAdapter;
import com.intellij.lang.puppet.lexer.PuppetLexerAdapter;
import com.intellij.lexer.LayeredLexer;
import com.intellij.openapi.project.Project;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.Nullable;

import static com.intellij.lang.puppet.PuppetTokenTypes.HEREDOC_BODY_QQ;

/**
 * @author Anna Bulenkova
 */
public class PuppetHighlightingLexer extends LayeredLexer {
  private static final IElementType[] HERDOC_ARRAY = new IElementType[]{HEREDOC_BODY_QQ};

  public PuppetHighlightingLexer(@Nullable Project project) {
    super(new PuppetLexerAdapter(project));

    registerSelfStoppingLayer(
      new PuppetHeredocLexerAdapter(project),
      HERDOC_ARRAY,
      IElementType.EMPTY_ARRAY
    );
  }
}
