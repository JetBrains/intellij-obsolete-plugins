package com.intellij.lang.puppet.psi;

import com.intellij.lang.ASTNode;
import com.intellij.lang.ParserDefinition;
import com.intellij.lang.PsiParser;
import com.intellij.lang.puppet.PuppetLanguage;
import com.intellij.lang.puppet.PuppetParser;
import com.intellij.lang.puppet.lexer.PuppetLexerAdapter;
import com.intellij.lang.puppet.lexer.PuppetTokenTypeSets;
import com.intellij.lang.puppet.psi.impl.PuppetCompositePsiElementBase;
import com.intellij.lexer.Lexer;
import com.intellij.openapi.project.Project;
import com.intellij.psi.FileViewProvider;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.tree.IFileElementType;
import com.intellij.psi.tree.IStubFileElementType;
import com.intellij.psi.tree.TokenSet;
import org.jetbrains.annotations.NotNull;

import static com.intellij.lang.puppet.PuppetTokenTypes.Factory;
import static com.intellij.lang.puppet.PuppetTokenTypes.HEREDOC_BODY_QQ;
import static com.intellij.lang.puppet.PuppetTokenTypes.STRING;

/**
 * @author Anna Bulenkova
 */
public class PuppetParserDefinition implements ParserDefinition {
  private static final IFileElementType PUPPET_FILE = new IStubFileElementType("PUPPET_FILE", PuppetLanguage.INSTANCE) {
    @Override
    public int getStubVersion() {
      return super.getStubVersion() + 1;
    }
  };

  @Override
  public @NotNull Lexer createLexer(Project project) {
    return new PuppetLexerAdapter(project);
  }

  @Override
  public @NotNull PsiParser createParser(Project project) {
    return new PuppetParser();
  }

  @Override
  public @NotNull IFileElementType getFileNodeType() {
    return PUPPET_FILE;
  }

  @Override
  public @NotNull TokenSet getCommentTokens() {
    return PuppetTokenTypeSets.COMMENTS;
  }

  @Override
  public @NotNull TokenSet getStringLiteralElements() {
    return TokenSet.create(STRING);
  }

  @Override
  public @NotNull PsiElement createElement(ASTNode node) {
    if (node.getElementType() == HEREDOC_BODY_QQ) {
      return new PuppetCompositePsiElementBase(node);
    }

    return Factory.createElement(node);
  }

  @Override
  public @NotNull PsiFile createFile(@NotNull FileViewProvider viewProvider) {
    return new PuppetPsiFileImpl(viewProvider);
  }

  @Override
  public @NotNull SpaceRequirements spaceExistenceTypeBetweenTokens(ASTNode left, ASTNode right) {
    return SpaceRequirements.MAY;
  }
}
