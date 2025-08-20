package com.intellij.play.language;

import com.intellij.lang.ASTNode;
import com.intellij.lang.ParserDefinition;
import com.intellij.lang.PsiParser;
import com.intellij.lexer.Lexer;
import com.intellij.openapi.project.Project;
import com.intellij.play.language.groovy.GroovyExpressionLazyParseableElementType;
import com.intellij.play.language.lexer.PlayLexer;
import com.intellij.play.language.psi.PlayPsiFile;
import com.intellij.psi.FileViewProvider;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.IFileElementType;
import com.intellij.psi.tree.TokenSet;
import org.jetbrains.annotations.NotNull;

public class PlayParserDefinition implements ParserDefinition {

  @Override
  @NotNull
  public Lexer createLexer(Project project) {
    return PlayLexer.createLexer();
  }

  @Override
  public @NotNull PsiParser createParser(Project project) {
    return new PlayParser();
  }

  @Override
  public @NotNull IFileElementType getFileNodeType() {
    return PlayElementTypes.PLAY_FILE;
  }

  @Override
  @NotNull
  public TokenSet getWhitespaceTokens() {
    return ParserDefinition.super.getWhitespaceTokens();
  }

  @Override
  @NotNull
  public TokenSet getCommentTokens() {
    return TokenSet.create(PlayElementTypes.COMMENT_START, PlayElementTypes.COMMENT_END, PlayElementTypes.COMMENT_TEXT);
  }

  @Override
  @NotNull
  public TokenSet getStringLiteralElements() {
    return PlayElementTypes.STRING_LITERALS;
  }

  @Override
  @NotNull
  public PsiElement createElement(ASTNode node) {
    final IElementType type = node.getElementType();

    if (type instanceof PlayCompositeElementType) {
      return ((PlayCompositeElementType)type).createPsiElement(node);
    }
    else if (type instanceof PlayActionElementType) {
      return new PlayActionCompositeElement(node);
    }
    else if (type instanceof TagExpressionElementType) {
      return new TagExpressionCompositeElement(node);
    }
    else if (type instanceof GroovyExpressionLazyParseableElementType) {
      return new PlayCompositeGroovyExpressionElement(node);
    }
    throw new AssertionError("Unknown type: " + type);
  }

  @Override
  public @NotNull PsiFile createFile(@NotNull FileViewProvider viewProvider) {
    return new PlayPsiFile(viewProvider);
  }

  @Override
  public @NotNull SpaceRequirements spaceExistenceTypeBetweenTokens(ASTNode left, ASTNode right) {
    return SpaceRequirements.MAY;
  }

  @Override
  public String toString() {
    return PlayParserDefinition.class.getName();
  }
}

